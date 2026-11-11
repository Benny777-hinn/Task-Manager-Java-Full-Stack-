package com.taskmanager.dto;

import com.taskmanager.entity.Role;

/**
 * Response returned after a successful register/login.
 * The frontend stores "token" in localStorage and sends it on every request.
 */
public record AuthResponse(
        String token,
        String tokenType,
        Long id,
        String username,
        String email,
        Role role
) {
}
