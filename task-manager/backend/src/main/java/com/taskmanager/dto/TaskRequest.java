package com.taskmanager.dto;

import com.taskmanager.entity.Priority;
import com.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request body for creating or updating a task.
 * categoryId is optional: pass null to leave the task "Uncategorized".
 */
public record TaskRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotNull(message = "Due date is required")
        LocalDateTime dueDate,

        @NotNull(message = "Priority is required")
        Priority priority,

        // Optional. If omitted we keep PENDING.
        TaskStatus status,

        // Optional category id owned by the same user.
        Long categoryId
) {
}
