package com.example.liveclass.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "에러 응답 DTO")
public class ErrorResponse {

    @Schema(description = "발생 시간", example = "2026-05-20T17:34:48")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP 상태 코드", example = "400")
    private int status;

    @Schema(description = "에러 타입", example = "Bad Request")
    private String error;

    @Schema(description = "에러 메시지", example = "입력 데이터 유효성 검증 실패")
    private String message;

    @Schema(description = "예외 클래스명", example = "MethodArgumentNotValidException")
    private String exceptionType;

    @Schema(description = "필드별 검증 에러 (Validation Error만)")
    private Map<String, String> validationErrors;

    @Schema(description = "스택트레이스 (디버깅용)")
    private String stackTrace;
}