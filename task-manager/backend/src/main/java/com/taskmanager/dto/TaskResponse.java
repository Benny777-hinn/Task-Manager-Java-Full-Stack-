package com.taskmanager.dto;

import com.taskmanager.entity.Priority;
import com.taskmanager.entity.TaskStatus;

import java.time.LocalDateTime;

/**
 * Response sent to the client for a task.
 *
 * We return a flat DTO instead of the Task entity on purpose:
 *  - the entity has lazy links (owner, category) that would cause lazy-loading errors,
 *  - the entity would leak internal fields (e.g. the owner's password),
 *  - a DTO lets us add computed fields like "overdue".
 */
public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDateTime dueDate,
        Priority priority,
        TaskStatus status,
        Long categoryId,
        String categoryName,
        Long ownerId,
        String ownerUsername,
        LocalDateTime createdAt,
        boolean reminderSent,
        boolean overdue
) {
}
