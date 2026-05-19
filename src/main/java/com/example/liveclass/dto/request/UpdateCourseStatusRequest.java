package com.example.liveclass.dto.request;

import com.example.liveclass.entity.Course.CourseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 강의 상태 변경 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Schema(description = "강의 상태 변경 요청")
public class UpdateCourseStatusRequest {

    @NotNull(message = "상태는 필수입니다")
    @Schema(description = "강의 상태 (DRAFT, OPEN, CLOSED)", example = "OPEN", requiredMode = Schema.RequiredMode.REQUIRED)
    private CourseStatus status;
}