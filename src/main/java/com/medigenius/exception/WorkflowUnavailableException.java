package com.medigenius.exception;

/**
 * Thrown when the agentic workflow cannot process a request (e.g. required AI backend
 * unreachable). Roughly equivalent to the HTTPException(status_code=503, ...) raised in
 * chat.py when chat_service.workflow_app was not initialized.
 */
public class WorkflowUnavailableException extends RuntimeException {
    public WorkflowUnavailableException(String message) {
        super(message);
    }

    public WorkflowUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
