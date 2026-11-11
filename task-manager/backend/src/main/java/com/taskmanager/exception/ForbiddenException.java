package com.taskmanager.exception;

/**
 * Thrown when a logged-in user tries to touch someone else's data.
 * For example a USER trying to read another user's task. -> 403 response.
 *
 * We check this ourselves in the service layer ("ownership check").
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
