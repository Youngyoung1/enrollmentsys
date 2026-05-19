package com.example.liveclass.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 강의 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "강의 생성 요청")
public class CreateCourseRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Schema(description = "강의 제목", example = "Spring Boot 실전 마스터", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "설명은 필수입니다")
    @Schema(description = "강의 설명", example = "REST API 개발을 배우는 실전 강좌입니다", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @NotNull(message = "가격은 필수입니다")
    @Positive(message = "가격은 0보다 커야 합니다")
    @Schema(description = "강의 가격 (원)", example = "50000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer price;

    @NotNull(message = "최대 정원은 필수입니다")
    @Positive(message = "최대 정원은 0보다 커야 합니다")
    @Max(value = 1000, message = "최대 정원은 1000명 이하여야 합니다")
    @Schema(description = "강의 최대 정원", example = "30", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer maxCapacity;

    @NotNull(message = "강의 시작일은 필수입니다")
    @FutureOrPresent(message = "강의 시작일은 현재 또는 미래 날짜여야 합니다")
    @Schema(description = "강의 시작일시 (ISO 8601 형식)", example = "2025-06-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startDate;

    @NotNull(message = "강의 종료일은 필수입니다")
    @FutureOrPresent(message = "강의 종료일은 현재 또는 미래 날짜여야 합니다")
    @Schema(description = "강의 종료일시 (ISO 8601 형식)", example = "2025-07-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endDate;
}
