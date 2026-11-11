package com.taskmanager.exception;

/**
 * Thrown when a resource (task, category, user) does not exist.
 * The GlobalExceptionHandler turns this into a 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
