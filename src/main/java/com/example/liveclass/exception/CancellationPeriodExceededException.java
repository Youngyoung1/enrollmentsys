package com.example.liveclass.exception;

/**
 * 취소 기간을 초과했을 때 발생하는 예외
 */
public class CancellationPeriodExceededException extends RuntimeException {
    public CancellationPeriodExceededException(String message) {
        super(message);
    }

    public CancellationPeriodExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}