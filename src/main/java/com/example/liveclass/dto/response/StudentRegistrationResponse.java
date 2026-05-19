package com.example.liveclass.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 학생 회원가입 응답 DTO
 * 학생 가입 후 UUID를 포함하여 반환
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRegistrationResponse {

    private String studentId;           // ✅ 학생 ID (student-1, student-2, ...)
    private String name;                // 학생명
    private String email;               // 이메일
    private String bio;                 // 학생 소개
    private String phone;               // 전화번호 (010-XXXX-XXXX)
    private LocalDateTime enrolledAt;   // 가입 시간
    private String message;             // "학생 회원가입 성공"
}