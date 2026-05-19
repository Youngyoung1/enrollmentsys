package com.example.liveclass.dto.response;

import com.example.liveclass.entity.Enrollment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 수강 신청 응답 DTO
 * 신청된 Enrollment의 UUID를 포함하여 반환
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {

    private String enrollmentId;        // ✅ Enrollment UUID (핵심!)
    private String userId;              // 학생 ID
    private String courseId;            // 강의 ID
    private String courseName;          // 강의명 (참고용)
    private String status;              // 신청 상태 (PENDING, CONFIRMED, CANCELLED)
    private LocalDateTime enrolledAt;   // 신청 시간
    private LocalDateTime confirmedAt;  // 결제 확정 시간
    private LocalDateTime cancelledAt;  // 취소 시간
    private String message;             // "수강 신청 성공" 등

    /**
     * Enrollment Entity를 Response로 변환
     */
    public static EnrollmentResponse from(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .enrollmentId(enrollment.getId())  // ✅ UUID 반환
                .userId(enrollment.getUserId())
                .courseId(enrollment.getCourseId())
                .status(enrollment.getStatus().toString())
                .enrolledAt(enrollment.getEnrolledAt())
                .confirmedAt(enrollment.getConfirmedAt())
                .cancelledAt(enrollment.getCancelledAt())
                .build();
    }
}