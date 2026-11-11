package com.taskmanager.dto;

/**
 * Response for a category. We never send the owner entity itself, only simple fields.
 */
public record CategoryResponse(
        Long id,
        String name,
        Long ownerId
) {
}
