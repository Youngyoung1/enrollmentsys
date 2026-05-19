package com.example.liveclass.entity;

import lombok.Getter;

/**
 * 신청 상태 열거형
 * PENDING: 신청 완료, 결제 대기
 * CONFIRMED: 결제 완료, 수강 확정
 * CANCELLED: 취소됨
 */
@Getter
public enum EnrollmentStatus {
    PENDING("대기중"),
    CONFIRMED("확정"),
    CANCELLED("취소");

    private final String description;

    EnrollmentStatus(String description) {
        this.description = description;
    }

}