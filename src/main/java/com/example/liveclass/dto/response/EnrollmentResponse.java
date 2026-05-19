package com.example.liveclass.dto.response;

import com.example.liveclass.entity.Enrollment;
import com.example.liveclass.entity.Enrollment.EnrollmentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 수강 신청 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "수강 신청 정보")
public class EnrollmentResponse {

    @Schema(description = "신청 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "학생 ID", example = "student-1")
    private String studentId;

    @Schema(description = "강의 ID", example = "550e8400-e29b-41d4-a716-446655440001")
    private String courseId;

    @Schema(description = "강의 제목", example = "Spring Boot 실전")
    private String courseTitle;

    @Schema(description = "신청 상태", example = "PENDING")
    private EnrollmentStatus status;

    @Schema(description = "확정 시간", example = "2026-05-19T02:23:51.541Z")
    private LocalDateTime confirmedAt;

    @Schema(description = "취소 시간", example = "2026-05-19T02:23:51.541Z")
    private LocalDateTime cancelledAt;

    @Schema(description = "신청 시간", example = "2026-05-19T02:23:51.541Z")
    private LocalDateTime enrolledAt;

    @Schema(description = "생성 시간", example = "2026-05-19T02:23:51.541Z")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시간", example = "2026-05-19T02:23:51.541Z")
    private LocalDateTime updatedAt;

    /**
     * Entity → Response 변환
     */
    public static EnrollmentResponse from(Enrollment entity) {
        return EnrollmentResponse.builder()
                .id(entity.getId())
                .studentId(entity.getStudent().getId())
                .courseId(entity.getCourse().getId())
                .courseTitle(entity.getCourse().getTitle())
                .status(entity.getStatus())
                .confirmedAt(entity.getConfirmedAt())
                .cancelledAt(entity.getCancelledAt())
                .enrolledAt(entity.getEnrolledAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
