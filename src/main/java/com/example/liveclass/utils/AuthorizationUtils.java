package com.example.liveclass.utils;

/**
 * Authorization 헤더에서 사용자 ID 추출
 * Swagger UI: "Bearer creator-1" → "creator-1"
 * Postman:    "creator-1"         → "creator-1"
 */
public class AuthorizationUtils {

    public static String extractUserId(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return "";
        }
        // "Bearer creator-1" 형태면 Bearer 제거
        if (authorization.startsWith("Bearer ") || authorization.startsWith("bearer ")) {
            return authorization.substring(7).trim();
        }
        // "creator-1" 형태면 그대로 반환
        return authorization.trim();
    }
}