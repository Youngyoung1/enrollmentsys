package com.example.liveclass.dto.response;

import com.example.liveclass.entity.Enrollment;
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
public class EnrollmentResponse {

    private String enrollmentId;        // 신청 UUID
    private String userId;              // 학생 ID
    private String courseId;            // 강의 ID
    private Integer queuePosition;      // ✅ 순번 (1부터)
    private String status;              // PENDING/WAITING/CONFIRMED/CANCELLED
    private LocalDateTime enrolledAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private String message;

    public static EnrollmentResponse from(Enrollment enrollment) {
        String message = switch (enrollment.getStatus()) {
            case PENDING -> "결제 대기 중입니다 (순번: " + enrollment.getQueuePosition() + ")";
            case WAITING -> "대기열에 등록되었습니다 (순번: " + enrollment.getQueuePosition() + ")";
            case CONFIRMED -> "수강이 확정되었습니다";
            case CANCELLED -> "취소되었습니다";
        };

        return EnrollmentResponse.builder()
                .enrollmentId(enrollment.getId())
                .userId(enrollment.getUserId())
                .courseId(enrollment.getCourseId())
                .queuePosition(enrollment.getQueuePosition())  // ✅
                .status(enrollment.getStatus().toString())
                .enrolledAt(enrollment.getEnrolledAt())
                .confirmedAt(enrollment.getConfirmedAt())
                .cancelledAt(enrollment.getCancelledAt())
                .message(message)
                .build();
    }
}