package com.example.liveclass.exception;

import com.example.liveclass.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Hidden  // ⭐ 클래스 레벨에 @Hidden 추가 - Swagger가 무시
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalException {

    /**
     * 정적 자원 없음 - 단순 404 반환
     */
    @Hidden  // ⭐ 메서드에도 @Hidden 추가
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    /**
     * 입력값 검증 실패
     */
    @Hidden
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                validationErrors.put(error.getField(), error.getDefaultMessage())
        );
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("입력 데이터 유효성 검증 실패")
                .exceptionType(MethodArgumentNotValidException.class.getSimpleName())
                .validationErrors(validationErrors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 비즈니스 예외
     */
    @Hidden
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatusCode())
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .exceptionType(ex.getClass().getSimpleName())
                .stackTrace(getStackTrace(ex))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(ex.getStatusCode()));
    }

    /**
     * 기타 모든 예외
     */
    @Hidden
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("서버 오류가 발생했습니다")
                .exceptionType(ex.getClass().getSimpleName())
                .stackTrace(getStackTrace(ex))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String getStackTrace(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stack = ex.getStackTrace();
        int lines = Math.min(15, stack.length);
        for (int i = 0; i < lines; i++) {
            sb.append("at ").append(stack[i].toString()).append("\n");
        }
        Throwable cause = ex.getCause();
        if (cause != null) {
            sb.append("Caused by: ").append(cause.toString()).append("\n");
        }
        return sb.toString();
    }
}