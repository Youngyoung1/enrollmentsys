package com.example.liveclass.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.example.liveclass.entity.Enrollment;
import com.example.liveclass.entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신청 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("courseId")
    private String courseId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("status")
    private EnrollmentStatus status;

    @JsonProperty("confirmedAt")
    private LocalDateTime confirmedAt;

    @JsonProperty("cancelledAt")
    private LocalDateTime cancelledAt;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @JsonProperty("course")
    private SimpleClassResponse course;

    /**
     * Enrollment 엔티티에서 Response DTO로 변환
     */
    public static EnrollmentResponse from(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .courseId(enrollment.getCourseId())
                .userId(enrollment.getUserId())
                .status(enrollment.getStatus())
                .confirmedAt(enrollment.getConfirmedAt())
                .cancelledAt(enrollment.getCancelledAt())
                .createdAt(enrollment.getCreatedAt())
                .updatedAt(enrollment.getUpdatedAt())
                .build();
    }

    /**
     * 간단한 강의 정보 DTO (중첩)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SimpleClassResponse {

        @JsonProperty("id")
        private String id;

        @JsonProperty("title")
        private String title;

        @JsonProperty("price")
        private Integer price;

        @JsonProperty("startDate")
        private LocalDateTime startDate;

        @JsonProperty("endDate")
        private LocalDateTime endDate;
    }
}
