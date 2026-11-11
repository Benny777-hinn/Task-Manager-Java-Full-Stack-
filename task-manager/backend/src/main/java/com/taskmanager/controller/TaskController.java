package com.taskmanager.controller;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.Priority;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for a user's own tasks.
 *
 * "Authentication" is injected by Spring Security and tells us who is logged in.
 * We pass authentication.getName() (the username) down to the service, which
 * performs the ownership checks.
 */
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Create and manage your own tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * GET /api/tasks
     * Optional filters: status, priority, categoryId, keyword.
     * Pagination: page (0-based), size, sortDir (asc/desc) - sorted by due date.
     */
    @GetMapping
    @Operation(summary = "List your tasks (filter, search, paginate, sort by due date)")
    public ResponseEntity<Page<TaskResponse>> getTasks(
            Authentication authentication,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Always sort by due date; the client only chooses the direction.
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "dueDate"));

        Page<TaskResponse> tasks = taskService.getUserTasks(
                authentication.getName(), status, priority, categoryId, keyword, pageable);

        return ResponseEntity.ok(tasks);
    }

    /** GET /api/tasks/{id} -> a single task you own. */
    @GetMapping("/{id}")
    @Operation(summary = "Get one of your tasks by id")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(taskService.getTask(id, authentication.getName()));
    }

    /** POST /api/tasks -> create a new task. */
    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request,
                                                   Authentication authentication) {
        TaskResponse created = taskService.createTask(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** PUT /api/tasks/{id} -> update one of your tasks. */
    @PutMapping("/{id}")
    @Operation(summary = "Update one of your tasks")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id,
                                                   @Valid @RequestBody TaskRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(taskService.updateTask(id, request, authentication.getName()));
    }

    /** DELETE /api/tasks/{id} -> delete one of your tasks. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete one of your tasks")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Authentication authentication) {
        taskService.deleteTask(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /** PATCH /api/tasks/{id}/complete -> mark a task as done. */
    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark a task as completed")
    public ResponseEntity<TaskResponse> completeTask(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(taskService.completeTask(id, authentication.getName()));
    }
}
