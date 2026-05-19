package com.example.liveclass.exception;

/**
 * 강의를 찾을 수 없을 때 발생하는 예외
 */
public class CourseNotFoundException extends ApiException {

    public CourseNotFoundException(String courseId) {
        super(
                "COURSE_NOT_FOUND",
                "강의를 찾을 수 없습니다. ID: " + courseId,
                404
        );
    }

    public CourseNotFoundException(String message, String courseId) {
        super(
                "COURSE_NOT_FOUND",
                message,
                404
        );
    }
}
