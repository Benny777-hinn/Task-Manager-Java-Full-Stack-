# Task Manager

A full-stack Task Manager application: **Spring Boot 3** REST API with **JWT authentication**,
role-based access (USER / ADMIN), MySQL, and a **React (Vite)** single-page frontend.
It also sends **hourly email reminders** for tasks that are due within the next 24 hours.

The code is written to be simple and readable, so it is easy to explain in an interview.
Short comments in the code explain the *why*, not just the *what*.

---

## 1. Tech stack

| Layer      | Technology |
|------------|------------|
| Backend    | Java 17, Spring Boot 3, Maven |
| Backend libs | Spring Web, Spring Data JPA, Spring Security, Spring Validation, Spring Mail |
| Database   | MySQL (H2 for tests) |
| Auth       | JWT (jjwt) + BCrypt password hashing, roles USER / ADMIN |
| API docs   | springdoc-openapi (Swagger UI) |
| Frontend   | React 18, Vite, React Router, Axios, plain CSS |
| Testing    | JUnit 5, Mockito, Spring Boot Test + MockMvc |
| DevOps     | Docker, docker-compose, GitHub Actions |

---

## 2. Architecture

```
+-------------------+        HTTP + JSON (JWT in header)        +----------------------+
|   React (Vite)    |  -------------------------------------->  |   Spring Boot API    |
|  - Pages          |     Authorization: Bearer <token>         |  - Controllers       |
|  - AuthContext    |  <--------------------------------------  |  - Services          |
|  - Axios client   |                                            |  - Repositories (JPA) |
+-------------------+                                            +----------+-----------+
         ^                                                                  |
         |                                                                  | SQL
         | served by nginx (production) / Vite dev server (dev)              v
         |                                                       +----------------------+
   Browser  <---------------------- HTML/CSS/JS -----------------|       MySQL          |
                                                                 +----------------------+
                                                                               ^
                                                                               |
                                                          @Scheduled hourly job+--> SMTP
                                                          (ReminderScheduler)      (MailHog / Gmail)
```

Request flow (layered architecture, package by layer):

```
HTTP request
   |
   v
JwtAuthenticationFilter  -> validates the token, sets the logged-in user
   |
   v
Controller (@RestController) -> validates the DTO, gets the username
   |
   v
Service (@Service) -> business rules + OWNERSHIP checks + transactions
   |
   v
Repository (Spring Data JPA) -> SQL
   |
   v
MySQL
```

---

## 3. Entity relationship (ER) diagram

```
            +--------------------+                +---------------------+
            |       users        |                |     categories      |
            +--------------------+                +---------------------+
            | id (PK)            |<---------------| id (PK)             |
            | username (unique)  |  1           * | name                |
            | email (unique)     |                | owner_id (FK)       |
            | password (bcrypt)  |                +---------------------+
            | role (USER/ADMIN)  |                          ^
            | created_at         |                          | 0..1
            +--------------------+                          |
                     ^                                       |
                     | 1                                     |
                     |                                       |
                     | *                                     | *
            +------------------------------------------------------+
            |                        tasks                         |
            +------------------------------------------------------+
            | id (PK)                                              |
            | title                                                |
            | description                                          |
            | due_date                                             |
            | priority (LOW/MEDIUM/HIGH)                           |
            | status (PENDING/COMPLETED)                           |
            | reminder_sent (boolean)                              |
            | created_at                                           |
            | owner_id (FK -> users.id)        NOT NULL            |
            | category_id (FK -> categories.id) NULLABLE           |
            +------------------------------------------------------+
```

Relationships:

- One **User** has many **Tasks** (`users 1 - * tasks`).
- One **User** has many **Categories** (`users 1 - * categories`).
- One **Category** can be used by many **Tasks**; a task may have no category (`categories 1 - * tasks`, optional).
- Deleting a user cascades to their tasks and categories.
- A unique constraint on `(name, owner_id)` stops a user from creating duplicate category names.

---

## 4. Project layout

```
task-manager/
├── backend/                     # Spring Boot application
│   ├── pom.xml
│   ├── Dockerfile               # multi-stage build
│   └── src/main/java/com/taskmanager/
│       ├── TaskManagerApplication.java
│       ├── controller/          # Auth, Task, Category, Admin controllers
│       ├── service/             # Auth, Task, Category, Admin, Email services
│       ├── repository/          # Spring Data JPA repositories
│       ├── entity/              # User, Role, Task, TaskStatus, Priority, Category
│       ├── dto/                 # request/response records
│       ├── security/            # JwtService, JwtAuthenticationFilter, UserDetailsService
│       ├── config/              # SecurityConfig, OpenApiConfig, DataInitializer
│       ├── exception/           # GlobalExceptionHandler + custom exceptions
│       └── scheduler/           # ReminderScheduler
├── frontend/                    # React (Vite) application
│   ├── package.json, vite.config.js, index.html
│   ├── Dockerfile, nginx.conf
│   └── src/ (api, context, components, pages, styles.css)
├── .github/workflows/ci.yml
├── docker-compose.yml
├── .env.example
├── README.md
└── INTERVIEW_NOTES.md
```

---

## 5. REST API

Base URL: `http://localhost:8081`

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST   | `/api/auth/register` | public | Register, returns a JWT |
| POST   | `/api/auth/login` | public | Login, returns a JWT |
| GET    | `/api/tasks` | USER | List **your** tasks (filter + search + paginate + sort) |
| GET    | `/api/tasks/{id}` | USER | Get one of your tasks |
| POST   | `/api/tasks` | USER | Create a task |
| PUT    | `/api/tasks/{id}` | USER | Update your task |
| DELETE | `/api/tasks/{id}` | USER | Delete your task |
| PATCH  | `/api/tasks/{id}/complete` | USER | Mark your task completed |
| GET    | `/api/categories` | USER | List your categories |
| POST   | `/api/categories` | USER | Create a category |
| DELETE | `/api/categories/{id}` | USER | Delete your category |
| GET    | `/api/admin/users` | ADMIN | List all users |
| DELETE | `/api/admin/users/{id}` | ADMIN | Delete a user |
| GET    | `/api/admin/tasks` | ADMIN | List all tasks |

Query parameters for `GET /api/tasks`:

| Parameter | Example | Meaning |
|-----------|---------|---------|
| `status` | `PENDING` | filter by status |
| `priority` | `HIGH` | filter by priority |
| `categoryId` | `2` | filter by category |
| `keyword` | `report` | case-insensitive search in title |
| `page` | `0` | page number (starts at 0) |
| `size` | `10` | tasks per page |
| `sortDir` | `asc` / `desc` | sort by due date |

Swagger UI: **http://localhost:8081/swagger-ui.html** → click **Authorize**, paste your JWT, then try endpoints.

---

## 6. How to run

### Option A — Docker (everything at once)

```bash
# 1. create your env file (only the first time)
cp .env.example .env        # Windows PowerShell: Copy-Item .env.example .env

# 2. build and start mysql, mailhog, backend and frontend
docker-compose up --build
```

Then open:

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| MailHog (emails) | http://localhost:8025 |

Stop everything with `docker-compose down` (add `-v` to also delete the database volume).

### Option B — Without Docker (local MySQL)

**1. Create the database** (once):

```sql
CREATE DATABASE taskmanager;
```

**2. Start the backend**

```bash
cd backend
# tell Spring how to reach your local MySQL
set DB_URL=jdbc:mysql://localhost:3306/taskmanager?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
set DB_USERNAME=root
set DB_PASSWORD=your_password
set JWT_SECRET=change-me-in-production-this-is-a-very-long-default-secret-key-1234567890
mvn spring-boot:run
```

Tables are created automatically (`spring.jpa.hibernate.ddl-auto=update`).

**3. Start the frontend** (in a second terminal)

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. The Vite dev server proxies `/api` to `http://localhost:8081`,
so no CORS setup is needed.

### Default admin account

On first start a beginner-friendly admin is created (change these in `.env`):

- username: `admin`
- password: `admin123`

Register normal users through the app's Register page.

### Email reminders

- The scheduler runs every hour (`REMINDER_CRON`, default `0 0 * * * *`).
- It finds **PENDING** tasks due in the next 24 hours with `reminderSent = false`,
  emails the owner, and sets `reminderSent = true` so each task is emailed only once.
- Locally, mail goes to **MailHog** (`http://localhost:8025`).
  For real Gmail, set `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_USERNAME`,
  `MAIL_PASSWORD` (an app password), `MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS=true`.

---

## 7. Tests

```bash
cd backend
mvn test
```

- **Unit tests (Mockito)**: `TaskServiceTest`, `AuthServiceTest` — repositories are mocked.
- **Integration tests (MockMvc)**: `AuthControllerIT`, `TaskControllerIT` — full context with **H2**,
  covering "token required", ownership rules, role restrictions and validation errors.

Total: 17 tests.

---

## 8. CI/CD

`.github/workflows/ci.yml` runs on every push and pull request:

1. **backend** — `mvn test`
2. **frontend** — `npm ci && npm run build`
3. **docker** — builds both Docker images

---

## 9. Screenshots

> Replace these placeholders with real screenshots after your first run.

| Screen | Preview |
|--------|---------|
| Login | `docs/screenshots/login.png` |
| Register | `docs/screenshots/register.png` |
| Dashboard (filters + task list) | `docs/screenshots/dashboard.png` |
| Add / Edit task | `docs/screenshots/task-form.png` |
| Categories | `docs/screenshots/categories.png` |
| Admin page | `docs/screenshots/admin.png` |
| Swagger UI | `docs/screenshots/swagger.png` |

---

## 10. Security notes

- Passwords are hashed with **BCrypt**; the plain password is never stored.
- The JWT secret and DB password come from **environment variables** — never hardcode them.
- `.env` is in `.gitignore`; only `.env.example` is committed.
- Every task/category lookup goes through an **ownership check** in the service layer, so a
  logged-in user can never read or modify another user's data.
