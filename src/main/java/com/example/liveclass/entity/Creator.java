package com.example.liveclass.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 강사 정보 테이블
 * Creator role을 가진 사용자의 추가 정보
 */
@Entity
@Table(name = "creator")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Creator {

    @Id
    private String id;  // creator-1, creator-2, ... (User의 id와 동일)

    @Column(columnDefinition = "TEXT")
    private String bio;  // 강사 소개

    private String expertise;  // 전문 분야

    private Integer totalStudents;  // 총 학생 수 (캐시)

    private Double avgRating;  // 평균 평점

    private LocalDateTime enrolledAt;  // 가입일

    private LocalDateTime updatedAt;
}