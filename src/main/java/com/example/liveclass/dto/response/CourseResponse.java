package com.example.liveclass.dto.response;

import com.example.liveclass.entity.Course;
import com.example.liveclass.entity.Course.CourseStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 강의 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "강의 정보")
public class CourseResponse {

    @Schema(description = "강의 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "강사 ID", example = "creator-1")
    private String creatorId;

    @Schema(description = "강의 제목", example = "Spring Boot 실전 마스터")
    private String title;

    @Schema(description = "강의 설명", example = "REST API 개발을 배웁니다")
    private String description;

    @Schema(description = "강의 가격 (원)", example = "50000")
    private Integer price;

    @Schema(description = "최대 정원", example = "30")
    private Integer maxCapacity;

    @Schema(description = "현재 신청 인원", example = "5")
    private Integer currentEnrollment;

    @Schema(description = "남은 자리 수", example = "25")
    private Integer availableSeats;

    @Schema(description = "강의 상태", example = "OPEN")
    private CourseStatus status;

    @Schema(description = "강의 시작일", example = "2025-06-01T10:00:00")
    private LocalDateTime startDate;

    @Schema(description = "강의 종료일", example = "2025-07-01T10:00:00")
    private LocalDateTime endDate;

    @Schema(description = "생성 시간", example = "2026-05-19T02:23:51.541Z")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시간", example = "2026-05-19T02:23:51.541Z")
    private LocalDateTime updatedAt;

    /**
     * Entity → Response 변환
     */
    public static CourseResponse from(Course entity) {
        return CourseResponse.builder()
                .id(entity.getId())
                .creatorId(entity.getCreator().getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .maxCapacity(entity.getMaxCapacity())
                .currentEnrollment(entity.getCurrentEnrollment())
                .availableSeats(entity.getMaxCapacity() - entity.getCurrentEnrollment())
                .status(entity.getStatus())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
