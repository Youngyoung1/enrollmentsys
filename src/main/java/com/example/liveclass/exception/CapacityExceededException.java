package com.example.liveclass.exception;

/**
 * 정원이 초과되었을 때 발생하는 예외
 */
public class CapacityExceededException extends ApiException {

    public CapacityExceededException(String courseId) {
        super(
                "CAPACITY_EXCEEDED",
                "강의 정원이 가득 찼습니다",
                400
        );
    }
}
