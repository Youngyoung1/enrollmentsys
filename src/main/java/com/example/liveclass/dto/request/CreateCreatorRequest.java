package com.example.liveclass.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 강사 회원가입 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCreatorRequest {

    @NotBlank(message = "사용자 ID는 필수입니다")
    @Size(min = 2, max = 50, message = "사용자 ID는 2-50자여야 합니다")
    private String id;  // creator-1, creator-2, ...

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 100, message = "이름은 1-100자여야 합니다")
    private String name;

    @Email(message = "유효한 이메일 주소여야 합니다")
    private String email;

    @Size(max = 500, message = "소개는 500자 이하여야 합니다")
    private String bio;

    @Size(max = 100, message = "전문 분야는 100자 이하여야 합니다")
    private String expertise;
}