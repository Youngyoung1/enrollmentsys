package com.example.liveclass.exception;

/**
 * 잘못된 상태 전이가 발생했을 때의 예외
 */
public class InvalidStateException extends ApiException {

    public InvalidStateException(String currentState, String targetState) {
        super(
                "INVALID_STATE_TRANSITION",
                "잘못된 상태 전이입니다. " + currentState + " -> " + targetState,
                400
        );
    }
}
