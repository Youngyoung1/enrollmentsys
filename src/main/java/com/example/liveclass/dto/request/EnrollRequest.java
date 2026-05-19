package com.example.liveclass.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 신청 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollRequest {

    @NotBlank(message = "강의 ID는 필수입니다")
    @JsonProperty("courseId")
    private String courseId;
}

