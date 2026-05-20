package com.example.liveclass.exception;

import lombok.Getter;

/**
 * API 예외의 기본 클래스
 */
@Getter
public class ApiException extends RuntimeException {

    private final String code;
    private final int status;

    public ApiException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public int getStatusCode() {
        return this.status;
    }

    public String getErrorCode() {
        return this.code;
    }
}