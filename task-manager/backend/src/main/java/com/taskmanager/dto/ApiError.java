package com.taskmanager.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The single JSON shape we return for every error, from GlobalExceptionHandler.
 * Having one consistent shape makes the frontend much easier to write.
 *
 * Example:
 * {
 *   "timestamp": "2026-11-08T10:15:30",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "path": "/api/tasks",
 *   "fieldErrors": { "title": "Title is required" }
 * }
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
