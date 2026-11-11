# Interview Notes — Task Manager

Simple explanations of how this project works, plus 15 likely interview questions.
Read this top to bottom; each section answers a "why", not just a "what".

---

## 1. Overall architecture

The app has three parts:

1. **Frontend (React + Vite)** — the screens the user sees. It calls the backend over HTTP
   and stores the JWT in `localStorage`.
2. **Backend (Spring Boot)** — a REST API. It is split into layers:
   `controller -> service -> repository -> database`.
3. **Database (MySQL)** — stores users, categories and tasks.

The backend follows a **layered architecture**:

| Layer | Class(es) | Responsibility |
|-------|-----------|----------------|
| Controller | `TaskController`, `AuthController`, ... | Receive HTTP, validate input, return JSON. No business logic. |
| Service | `TaskService`, `AuthService`, ... | Business rules, **ownership checks**, transactions. |
| Repository | `TaskRepository`, ... | Talk to the database (Spring Data JPA writes the SQL). |
| Entity | `Task`, `User`, `Category` | The database tables (JPA `@Entity`). |
| DTO | `TaskRequest`, `TaskResponse`, ... | The JSON shapes of the API (never the entities). |

Keeping these separate is why the code is easy to test: we can test the service with
mocked repositories, and the controllers with MockMvc.

---

## 2. JWT login flow, step by step

**Registration (`POST /api/auth/register`)**

1. `AuthController` receives the JSON and `@Valid` runs the bean-validation rules
   (username length, email format, password length).
2. `AuthService.register()` checks `existsByUsername` / `existsByEmail` and throws
   `ConflictException` (409) if they are taken.
3. The password is hashed with `passwordEncoder.encode(...)` (**BCrypt**), so the
   plain password is never stored.
4. The user is saved and the role defaults to `USER`.
5. A JWT is generated and returned along with basic user info.

**Login (`POST /api/auth/login`)**

1. `AuthService.login()` calls `authenticationManager.authenticate(...)` with the
   username and password.
2. Spring Security loads the user via `CustomUserDetailsService` and compares the
   entered password with the stored BCrypt hash (`PasswordEncoder.matches`).
3. If it does not match, `BadCredentialsException` is thrown and the
   `GlobalExceptionHandler` returns a clean **401** JSON response.
4. If it matches, `JwtService.generateToken(username, role)` creates the token and
   the frontend stores it in `localStorage`.

**Every later request**

1. Axios adds the header `Authorization: Bearer <token>` (request interceptor).
2. `JwtAuthenticationFilter` reads the header, verifies the signature, extracts the
   username, loads the user and puts an `Authentication` object into the
   `SecurityContext`.
3. The request continues to the controller, where `authentication.getName()` gives us
   the current username.

What is inside the JWT: `sub` (username), a `role` claim, `iat` (issued at) and
`exp` (expiry). The signature is created with our secret key, so the server can trust
the token without storing any session.

---

## 3. How Spring Security filters work in this project

Security is a **chain of filters** that runs before the request reaches a controller:

```
Request
  -> CORS filter
  -> JwtAuthenticationFilter           <-- OUR filter (added before the standard one)
  -> UsernamePasswordAuthenticationFilter (not used: we don't send username/password per request)
  -> ExceptionTranslationFilter        <-- turns security exceptions into 401 / 403
  -> FilterSecurityInterceptor / authorization
  -> Controller
```

Key points in `SecurityConfig`:

- `csrf().disable()` — we use JWTs, not cookies/sessions, so CSRF protection is not needed.
- `sessionCreationPolicy(STATELESS)` — the server keeps no session; every request proves
  identity with its token.
- `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` —
  our filter runs first and decides *who* the user is.
- `authorizeHttpRequests(...)` decides *what* each user may do.
- A custom `authenticationEntryPoint` returns **401** and an `accessDeniedHandler`
  returns **403** as JSON (instead of the default HTML page).

The filter itself is a `OncePerRequestFilter`, so it runs exactly once per request.

---

## 4. How role-based access is enforced

Two roles: **USER** and **ADMIN**.

1. **URL level (coarse)** — in `SecurityConfig`:
   ```
   .requestMatchers("/api/admin/**").hasRole("ADMIN")
   .anyRequest().authenticated()
   ```
   `hasRole("ADMIN")` checks for the authority `ROLE_ADMIN`. That authority is created in
   `CustomUserDetailsService` as `"ROLE_" + user.getRole().name()`.

2. **Data level (fine)** — "you may only touch your own data" is enforced in the
   **service layer** with ownership checks, e.g.:
   ```java
   taskRepository.findByIdAndOwnerId(id, owner.getId())
   ```
   If the task exists but belongs to someone else, the query returns nothing and we throw
   `ResourceNotFoundException` (**404**). We deliberately return 404 instead of 403 so an
   attacker cannot even tell whether the task exists.

3. **UI level** — the frontend only shows the **Admin** link when `user.role === 'ADMIN'`,
   and `ProtectedRoute` redirects non-admins. (The UI is only for convenience — the real
   protection is on the server.)

Why both layers? URL rules are cheap and clear, but they cannot express "only your own
rows". Ownership checks can. Real projects need both.

---

## 5. How the email scheduler works

`ReminderScheduler` is annotated with `@Scheduled(cron = "${app.reminder.cron}")`.
`@EnableScheduling` on the main class switches the scheduler on.

- Default cron `0 0 * * * *` = second 0, minute 0, every hour (Spring uses **6** fields,
  unlike Linux cron which uses 5).
- Each run:
  1. Compute `now` and `now + 24 hours`.
  2. Query tasks that are `PENDING`, `reminder_sent = false`, and due inside that window
     (`findTasksDueForReminder`, using `JOIN FETCH` so the owner is loaded in one query).
  3. For each task, send the email via `EmailService`, then set `reminderSent = true`
     and save.
- The `reminderSent` flag is what stops the same task being emailed every hour.
- We only set the flag **after** the email is sent, so if sending fails we retry next hour.
- Each task is wrapped in a `try/catch`, so one bad address cannot stop the whole job.

The retry control returns quickly; `reminderSent` is also reset implicitly by creating a
new task. For local testing the mail goes to **MailHog**; in production you would point
`MAIL_HOST` at Gmail with an app password.

> Note: `@Scheduled` runs in a single thread by default, so two runs never overlap. For a
> large system you would run this on one instance only (or use a distributed lock) so the
> job is not executed by every replica at once.

---

## 6. Why DTOs are used (and not the entities)

A DTO (Data Transfer Object) is the object we accept from and send to the client.
Here they are Java `record`s, e.g. `TaskRequest` and `TaskResponse`.

Reasons:

1. **Security** — returning the `User` entity would leak the password hash. DTOs expose
   only the fields we choose.
2. **No lazy-loading errors** — entities have lazy links (`owner`, `category`). Serializing
   them directly can throw `LazyInitializationException`. The DTO is a flat, fully-loaded
   object.
3. **Decoupling** — we can change the database/entity without breaking the API shape.
4. **Validation** — validation annotations live on the request DTO (`@NotBlank`, `@Email`),
   so bad input is rejected before it reaches the service.
5. **Computed fields** — the response can include things that are not columns, such as
   `overdue` and `categoryName`.

Mapping happens in one place: `TaskService.toResponse(...)`.

---

## 7. Database schema and relationships

Entities → tables: `users`, `categories`, `tasks`.

```
users(id, username UNIQUE, email UNIQUE, password, role, created_at)

categories(id, name, owner_id -> users.id)
   UNIQUE (name, owner_id)

tasks(id, title, description, due_date, priority, status,
      reminder_sent, created_at,
      owner_id -> users.id NOT NULL,
      category_id -> categories.id NULLABLE)
```

- `User 1 --- * Task` (`@ManyToOne owner` on Task).
- `User 1 --- * Category` (`@ManyToOne owner` on Category).
- `Category 1 --- * Task` (optional: a task may be uncategorized).
- `cascade = ALL, orphanRemoval = true` on the user's collections means deleting a user
  deletes their tasks and categories automatically.
- Enums are stored as **strings** (`@Enumerated(EnumType.STRING)`) so the database stays
  readable (`"HIGH"` instead of `2`).
- `ddl-auto=update` lets Hibernate create/update the tables from the entities while
  learning. In production you would use Flyway or Liquibase migrations.
- When a category is deleted, the service first sets `task.category = null` for its tasks,
  so the foreign key does not block the delete.

---

## 8. Fifteen likely interview questions

**Q1. Why JWT instead of server sessions?**
JWTs are stateless: the server does not store session data, which makes horizontal scaling
easy and works well for SPAs and mobile clients. The token carries the identity and is
verified with a signature.

**Q2. Where is the JWT stored on the client, and what is the trade-off?**
In `localStorage`. It is simple but readable by JavaScript, so a strong Content-Security-
Policy matters. The alternative is an httpOnly cookie, which is safer against XSS but needs
CSRF protection.

**Q3. How do you stop a user from reading another user's task?**
The repository method `findByIdAndOwnerId(id, ownerId)` always filters by the owner. If the
row is not found we throw `ResourceNotFoundException` (404), which also avoids revealing
that the task exists.

**Q4. What is the difference between authentication and authorization here?**
Authentication = "who are you" (the JWT filter + login). Authorization = "what may you do"
(`hasRole("ADMIN")` on `/api/admin/**`, plus ownership checks in the service).

**Q5. Why does `hasRole("ADMIN")` need the `ROLE_` prefix?**
Spring Security's `hasRole` automatically prefixes `ROLE_`. Our `CustomUserDetailsService`
grants the authority `ROLE_ADMIN`, so the two match. (`hasAuthority` would be used without
the prefix.)

**Q6. How are passwords protected?**
They are hashed with BCrypt (`BCryptPasswordEncoder`). BCrypt is a one-way, salted,
deliberately slow hash, which makes brute-forcing expensive. The plain password is never
stored or logged.

**Q7. Why is CSRF protection disabled?**
CSRF attacks rely on the browser automatically attaching cookies. We use a bearer token in
a header (not a cookie), so the browser will not attach it automatically and CSRF is not a
threat. Sessions are stateless.

**Q8. What does the JwtAuthenticationFilter do exactly?**
Reads `Authorization: Bearer <token>`, verifies the signature/expiry, loads the user, and
puts an `Authentication` into the `SecurityContext` for the rest of the request. If there
is no token it does nothing and the request ends as 401.

**Q9. Why use DTOs instead of returning entities?**
To avoid leaking sensitive fields (password), to prevent lazy-loading errors, to keep the
API contract stable, and to attach validation and computed fields (`overdue`). See section 6.

**Q10. How does filtering + pagination + sorting work?**
`GET /api/tasks` takes `status`, `priority`, `categoryId`, `keyword`, `page`, `size`,
`sortDir`. The service builds a JPA `Specification` (a dynamic WHERE clause) and passes a
`Pageable` (`PageRequest.of(page, size, Sort.by(direction, "dueDate"))`). Spring Data
returns a `Page` containing `content`, `totalPages`, `totalElements`.

**Q11. Why did you use `EnumType.STRING` for role/status/priority?**
Storing enums as numbers is fragile: reordering the enum silently changes the meaning of
existing rows. Strings stay readable and safe.

**Q12. How does the reminder job avoid sending duplicate emails?**
Each task has a `reminder_sent` flag. The query only selects rows where it is `false`, and
we set it to `true` after a successful send. A failure leaves it `false` so the next run
retries.

**Q13. How is a consistent error JSON achieved?**
`GlobalExceptionHandler` (`@RestControllerAdvice`) catches specific exceptions and returns
one `ApiError` shape for all of them: `timestamp`, `status`, `error`, `message`, `path`,
and `fieldErrors` for validation problems.

**Q14. What happens if the same category name is created twice?**
There is a unique constraint on `(name, owner_id)`. The service also checks
`existsByNameAndOwnerId` first and throws `ConflictException` (409), giving a friendly
message instead of a raw database error.

**Q15. How do the tests prove the security rules work?**
`TaskControllerIT` uses MockMvc to show that (a) a request without a token gets 401,
(b) a normal user gets 403 on `/api/admin/users`, and (c) user B gets 404 when reading
user A's task. `TaskServiceTest`/`AuthServiceTest` (Mockito) test the service logic in
isolation. Tests run on an in-memory **H2** database, so no real MySQL is needed.

---

## Quick recap to say out loud

> "It's a Spring Boot REST API secured with JWTs and two roles. Controllers are thin,
> services hold the rules and the ownership checks, repositories use Spring Data JPA over
> MySQL. The React frontend stores the token and sends it with Axios. A scheduled job
> emails reminders once per task. Everything is tested with Mockito unit tests and MockMvc
> integration tests, and it all ships with Docker and GitHub Actions."
