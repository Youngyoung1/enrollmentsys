package com.example.liveclass.exception;

/**
 * 신청을 찾을 수 없을 때 발생하는 예외
 */
public class EnrollmentNotFoundException extends ApiException {

    public EnrollmentNotFoundException(String enrollmentId) {
        super(
                "ENROLLMENT_NOT_FOUND",
                "신청을 찾을 수 없습니다. ID: " + enrollmentId,
                404
        );
    }
}
