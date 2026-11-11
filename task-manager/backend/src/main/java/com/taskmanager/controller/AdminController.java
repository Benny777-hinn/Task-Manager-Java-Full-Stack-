package com.taskmanager.controller;

import com.taskmanager.dto.TaskResponse;
import com.taskmanager.dto.UserResponse;
import com.taskmanager.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ADMIN-only endpoints.
 *
 * There is no role check inside this class: SecurityConfig already blocks
 * /api/admin/** for anyone without ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Admin-only endpoints (require the ADMIN role)")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** GET /api/admin/users -> list all users (paged). */
    @GetMapping("/users")
    @Operation(summary = "List all users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    /** DELETE /api/admin/users/{id} -> delete a user and all of their data. */
    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/admin/tasks -> list every task in the system (paged). */
    @GetMapping("/tasks")
    @Operation(summary = "List all tasks from all users")
    public ResponseEntity<Page<TaskResponse>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dueDate").ascending());
        return ResponseEntity.ok(adminService.getAllTasks(pageable));
    }
}
