package com.example.liveclass.exception;

/**
 * 인증이 실패했을 때 발생하는 예외
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(
                "UNAUTHORIZED",
                message,
                401
        );
    }

    public UnauthorizedException() {
        super(
                "UNAUTHORIZED",
                "인증이 필요합니다",
                401
        );
    }
}