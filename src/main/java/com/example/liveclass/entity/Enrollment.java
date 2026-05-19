package com.example.liveclass.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신청 엔티티
 * 사용자의 강의 신청 정보
 */
@Entity
@Table(name = "enrollment", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_course_id", columnList = "course_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_course_user_active",
        columnNames = {"course_id", "user_id", "status"}
))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "course_id", nullable = false, length = 36)
    private String courseId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 신청 상태를 CONFIRMED로 변경
     */
    public void confirm() {
        this.status = EnrollmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 신청 상태를 CANCELLED로 변경
     */
    public void cancel() {
        this.status = EnrollmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * PENDING 상태인지 확인
     */
    public Boolean isPending() {
        return status == EnrollmentStatus.PENDING;
    }

    /**
     * CONFIRMED 상태인지 확인
     */
    public Boolean isConfirmed() {
        return status == EnrollmentStatus.CONFIRMED;
    }
}