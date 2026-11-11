package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.Category;
import com.taskmanager.entity.Priority;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.User;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.repository.CategoryRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * All business logic for tasks.
 *
 * Two important responsibilities live here:
 *  1. Ownership checks: a normal user may only touch their own tasks.
 *  2. Building the dynamic filter query (status / priority / category / keyword).
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository,
                       CategoryRepository categoryRepository,
                       UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    /** Create a task owned by the currently logged-in user. */
    @Transactional
    public TaskResponse createTask(TaskRequest request, String username) {
        User owner = getCurrentUser(username);

        Task task = new Task();
        applyRequestToTask(task, request, owner);
        task.setOwner(owner);

        return toResponse(taskRepository.save(task));
    }

    /**
     * List the current user's tasks with optional filters, pagination and sorting.
     * The pageable (page number, size, sort) is built by the controller.
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getUserTasks(String username,
                                           TaskStatus status,
                                           Priority priority,
                                           Long categoryId,
                                           String keyword,
                                           Pageable pageable) {
        User owner = getCurrentUser(username);
        Specification<Task> spec = buildSpecification(owner.getId(), status, priority, categoryId, keyword);
        return taskRepository.findAll(spec, pageable).map(this::toResponse);
    }

    /** Used by the admin endpoints: every task from every user. */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasksForAdmin(Pageable pageable) {
        return taskRepository.findAll(pageable).map(this::toResponse);
    }

    /** Get one task, but only if it belongs to this user. */
    @Transactional(readOnly = true)
    public TaskResponse getTask(Long id, String username) {
        User owner = getCurrentUser(username);
        Task task = taskRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
        return toResponse(task);
    }

    /** Update a task (only its owner may do this). */
    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request, String username) {
        User owner = getCurrentUser(username);
        Task task = taskRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));

        applyRequestToTask(task, request, owner);
        return toResponse(taskRepository.save(task));
    }

    /** Delete a task (only its owner may do this). */
    @Transactional
    public void deleteTask(Long id, String username) {
        User owner = getCurrentUser(username);
        Task task = taskRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
        taskRepository.delete(task);
    }

    /** Mark a task as COMPLETED (only its owner may do this). */
    @Transactional
    public TaskResponse completeTask(Long id, String username) {
        User owner = getCurrentUser(username);
        Task task = taskRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));

        task.setStatus(TaskStatus.COMPLETED);
        return toResponse(taskRepository.save(task));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Load the User row for the username stored in the JWT. */
    private User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    /** Copy values from the request DTO onto the entity (used by create and update). */
    private void applyRequestToTask(Task task, TaskRequest request, User owner) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority());
        // Default to PENDING when the client does not send a status.
        task.setStatus(request.status() != null ? request.status() : TaskStatus.PENDING);

        // Resolve the optional category. It must belong to the same user,
        // otherwise the user could link their task to someone else's category.
        if (request.categoryId() != null) {
            Category category = categoryRepository.findByIdAndOwnerId(request.categoryId(), owner.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id " + request.categoryId()));
            task.setCategory(category);
        } else {
            task.setCategory(null);
        }
    }

    /**
     * Build a WHERE clause step by step. Each filter is added only if it is present,
     * so the same method works for "all tasks" and for "high priority, Work, title has 'report'".
     */
    private Specification<Task> buildSpecification(Long ownerId, TaskStatus status, Priority priority,
                                                   Long categoryId, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always limit to the current user's tasks.
            predicates.add(cb.equal(root.get("owner").get("id"), ownerId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (keyword != null && !keyword.isBlank()) {
                // Case-insensitive "contains" search on the title.
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), like));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Convert the entity into the flat DTO we send to the client. */
    public TaskResponse toResponse(Task task) {
        Category category = task.getCategory();
        // A task is "overdue" when it is still PENDING and its due date is in the past.
        boolean overdue = task.getStatus() == TaskStatus.PENDING
                && task.getDueDate() != null
                && task.getDueDate().isBefore(LocalDateTime.now());

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getPriority(),
                task.getStatus(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                task.getOwner().getId(),
                task.getOwner().getUsername(),
                task.getCreatedAt(),
                task.isReminderSent(),
                overdue
        );
    }
}
