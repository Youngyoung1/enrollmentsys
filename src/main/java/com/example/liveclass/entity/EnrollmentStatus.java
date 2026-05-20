package com.example.liveclass.entity;

import lombok.Getter;

/**
 * 신청 상태 열거형
 * PENDING:   정원 내, 결제 대기
 * WAITING:   정원 초과, 대기열
 * CONFIRMED: 결제 완료, 수강 확정
 * CANCELLED: 취소됨
 */
@Getter
public enum EnrollmentStatus {
    PENDING("결제대기"),
    WAITING("대기열"),
    CONFIRMED("확정"),
    CANCELLED("취소");

    private final String description;

    EnrollmentStatus(String description) {
        this.description = description;
    }
}