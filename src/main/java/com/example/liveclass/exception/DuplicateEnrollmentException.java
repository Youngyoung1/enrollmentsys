package com.example.liveclass.exception;

/**
 * 중복 신청이 발생했을 때 발생하는 예외
 */
public class DuplicateEnrollmentException extends ApiException {

    public DuplicateEnrollmentException(String courseId, String userId) {
        super(
                "DUPLICATE_ENROLLMENT",
                "이미 이 강의에 신청했습니다",
                409
        );
    }

    public DuplicateEnrollmentException(String message) {
        super(
                "DUPLICATE_ENROLLMENT",
                message,
                409
        );
    }
}








