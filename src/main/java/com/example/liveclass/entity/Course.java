package com.example.liveclass.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 강의 정보 테이블
 * 강의의 기본 정보 저장
 */
@Entity
@Table(name = "course")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    private String id;  // UUID

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;  // 강사 정보 (FK)

    @Column(nullable = false)
    private String title;  // 강의 제목

    @Column(columnDefinition = "TEXT")
    private String description;  // 강의 설명

    @Column(nullable = false)
    private Integer price;  // 강의 가격 (원)

    @Column(nullable = false)
    private Integer maxCapacity;  // 최대 정원

    @Column(nullable = false)
    private Integer currentEnrollment;  // 현재 신청 인원

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;  // 강의 상태

    @Column(nullable = false)
    private LocalDateTime startDate;  // 강의 시작일

    @Column(nullable = false)
    private LocalDateTime endDate;  // 강의 종료일

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성 시간

    private LocalDateTime updatedAt;  // 수정 시간

    /**
     * 강의 상태
     */
    public enum CourseStatus {
        DRAFT,   // 초안
        OPEN,    // 공개 중 (신청 가능)
        CLOSED   // 마감 (신청 불가)
    }
}