package com.taskmanager.exception;

/**
 * Thrown when the request is valid but conflicts with existing data,
 * e.g. registering a username that is already taken. -> 409 response.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
