package com.example.liveclass.entity;

import lombok.Getter;

/**
 * 강의 상태 열거형
 * DRAFT: 초안 (신청 불가)
 * OPEN: 모집 중 (신청 가능)
 * CLOSED: 모집 마감 (신청 불가)
 */
@Getter
public enum CourseStatus {
    DRAFT("초안"),
    OPEN("모집중"),
    CLOSED("모집마감");

    private final String description;

    CourseStatus(String description) {
        this.description = description;
    }

}
