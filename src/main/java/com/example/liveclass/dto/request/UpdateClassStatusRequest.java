package com.example.liveclass.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.liveclass.entity.ClassStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 강의 상태 변경 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateClassStatusRequest {

    @NotNull(message = "상태는 필수입니다")
    @JsonProperty("status")
    private ClassStatus status;
}
