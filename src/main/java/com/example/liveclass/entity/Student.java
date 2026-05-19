package com.example.liveclass.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 학생 정보 테이블
 * Student role을 가진 사용자의 추가 정보
 */
@Entity
@Table(name = "student")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    private String id;  // student-1, student-2, ... (User의 id와 동일)

    @Column(columnDefinition = "TEXT")
    private String bio;  // 학생 소개

    private String phone;  // 전화번호

    private LocalDateTime enrolledAt;  // 가입일

    private LocalDateTime updatedAt;
}