package com.example.liveclass.controller;

import com.example.liveclass.exception.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * 개발 환경: 상세 에러 메시지 노출
 */
@RestControllerAdvice
@Slf4j
public class GlobalException {

    @Data
    @Builder
    @AllArgsConstructor
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String detail;  // ✅ 추가: 상세 정보
        private String stackTrace;  // ✅ 추가: 스택 트레이스
    }

    // 400 - Bad Request
    @ExceptionHandler({
            InvalidStateException.class,
            DuplicateException.class,
            DuplicateEnrollmentException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException e) {
        log.error("Bad Request: ", e);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(e.getMessage())
                .detail(e.getClass().getSimpleName())
                .stackTrace(getStackTrace(e))  // ✅ 스택 트레이스 포함
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 401 - Unauthorized
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        log.error("Unauthorized: ", e);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(e.getMessage())
                .detail(e.getClass().getSimpleName())
                .stackTrace(getStackTrace(e))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 403 - Forbidden
    @ExceptionHandler(CancellationPeriodExceededException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(CancellationPeriodExceededException e) {
        log.error("Forbidden: ", e);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(e.getMessage())
                .detail(e.getClass().getSimpleName())
                .stackTrace(getStackTrace(e))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 404 - Not Found
    @ExceptionHandler({
            CourseNotFoundException.class,
            EnrollmentNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        log.error("Not Found: ", e);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(e.getMessage())
                .detail(e.getClass().getSimpleName())
                .stackTrace(getStackTrace(e))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 409 - Conflict
    @ExceptionHandler({
            CourseNotOpenException.class,
            CapacityExceededException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException e) {
        log.error("Conflict: ", e);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(e.getMessage())
                .detail(e.getClass().getSimpleName())
                .stackTrace(getStackTrace(e))
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 500 - Internal Server Error (모든 예외)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("=== 예상치 못한 에러 발생 ===", e);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(e.getMessage())
                .detail(e.getClass().getName())  // ✅ 풀 클래스명
                .stackTrace(getStackTrace(e))  // ✅ 전체 스택 트레이스
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 스택 트레이스를 문자열로 변환
     */
    private String getStackTrace(Throwable e) {
        return Arrays.stream(e.getStackTrace())
                .limit(10)  // 상위 10개만
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n  at "));
    }
}