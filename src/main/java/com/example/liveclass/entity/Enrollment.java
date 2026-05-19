package com.example.liveclass.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 수강 신청 정보 테이블
 */
@Entity
@Table(name = "enrollment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    private String id;  // UUID

    @Column(nullable = false)
    private String userId;  // 학생 ID

    @Column(nullable = false)
    private String courseId;  // 강의 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;  // 신청 상태

    @Column(nullable = false, updatable = false)
    private LocalDateTime enrolledAt;  // 신청 시간

    private LocalDateTime confirmedAt;  // 결제 확정 시간

    private LocalDateTime cancelledAt;  // 취소 시간

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성 시간

    private LocalDateTime updatedAt;  // 수정 시간
}