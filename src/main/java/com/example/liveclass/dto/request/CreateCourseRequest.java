package com.example.liveclass.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 강의 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Schema(description = "강의 생성 요청")
public class CreateCourseRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(min = 1, max = 255, message = "제목은 1자 이상 255자 이하여야 합니다")
    @Schema(description = "강의 제목", example = "liveclass채용과제", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "설명은 필수입니다")
    @Schema(description = "강의 설명", example = "BE-A", requiredMode = Schema.RequiredMode.REQUIRED)
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
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @FutureOrPresent(message = "강의 시작일은 현재 또는 미래 날짜여야 합니다")
    @Schema(
            description = "강의 시작일시 (yyyy-MM-dd'T'HH:mm:ss 형식, Z 형식 제거)",
            example = "2026-06-01T10:00:00",
            type = "string",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime startDate;

    @NotNull(message = "강의 종료일은 필수입니다")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @FutureOrPresent(message = "강의 종료일은 현재 또는 미래 날짜여야 합니다")
    @Schema(
            description = "강의 종료일시 (yyyy-MM-dd'T'HH:mm:ss 형식, Z 형식 제거)",
            example = "2026-07-01T10:00:00",
            type = "string",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime endDate;
}