package com.example.liveclass.exception;

/**
 * 중복 데이터 예외
 */
public class DuplicateException extends RuntimeException {
    public DuplicateException(String message) {
        super(message);
    }

    public DuplicateException(String message, Throwable cause) {
        super(message, cause);
    }
}