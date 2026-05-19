package com.example.liveclass.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 강사 회원가입 응답 DTO
 * 강사 가입 후 UUID를 포함하여 반환
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorRegistrationResponse {

    private String creatorId;           // ✅ 강사 ID (creator-1, creator-2, ...)
    private String name;                // 강사명
    private String email;               // 이메일
    private String bio;                 // 강사 소개
    private String expertise;           // 전문 분야
    private LocalDateTime enrolledAt;   // 가입 시간
    private String message;             // "강사 회원가입 성공"
}