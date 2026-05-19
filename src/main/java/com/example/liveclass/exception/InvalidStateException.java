package com.example.liveclass.exception;

/**
 * 신청 상태가 올바르지 않을 때 발생하는 예외
 */
public class InvalidStateException extends RuntimeException {
    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }
}