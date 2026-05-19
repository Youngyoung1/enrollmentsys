package com.example.liveclass.exception;

/**
 * 강의 상태가 OPEN이 아닐 때 발생하는 예외
 */
public class CourseNotOpenException extends ApiException {

    public CourseNotOpenException(String courseId, String currentStatus) {
        super(
                "COURSE_NOT_OPEN",
                "강의가 모집 중 상태가 아닙니다. 현재 상태: " + currentStatus,
                400
        );
    }

    public CourseNotOpenException(String message) {
        super(
                "COURSE_NOT_OPEN",
                message,
                400
        );
    }
}
