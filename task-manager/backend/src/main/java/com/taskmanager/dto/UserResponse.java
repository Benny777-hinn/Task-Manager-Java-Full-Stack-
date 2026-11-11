package com.taskmanager.dto;

import com.taskmanager.entity.Role;

import java.time.LocalDateTime;

/**
 * Response for a user, used by the ADMIN endpoints.
 * Note: we deliberately do NOT include the password field.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        LocalDateTime createdAt
) {
}
