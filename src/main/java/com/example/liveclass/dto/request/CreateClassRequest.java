package com.example.liveclass.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 강의 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClassRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(min = 1, max = 255, message = "제목은 1~255자여야 합니다")
    @JsonProperty("title")
    private String title;

    @NotBlank(message = "설명은 필수입니다")
    @Size(min = 1, max = 2000, message = "설명은 1~2000자여야 합니다")
    @JsonProperty("description")
    private String description;

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    @JsonProperty("price")
    private Integer price;

    @NotNull(message = "정원은 필수입니다")
    @Min(value = 1, message = "정원은 1 이상이어야 합니다")
    @Max(value = 10000, message = "정원은 10000 이하여야 합니다")
    @JsonProperty("maxCapacity")
    private Integer maxCapacity;

    @NotNull(message = "수강 시작일은 필수입니다")
    @JsonProperty("startDate")
    private LocalDateTime startDate;

    @NotNull(message = "수강 종료일은 필수입니다")
    @JsonProperty("endDate")
    private LocalDateTime endDate;
}
