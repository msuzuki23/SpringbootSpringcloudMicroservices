package com.msuzuki.api.exceptions;

public class EventProcessingException extends RuntimeException {
    public EventProcessingException() {}

    public EventProcessingException(String message) {
        super(message);
    }

    public EventProcessingException(Throwable cause) {
        super(cause);
    }

    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
