package com.example.liveclass.controller;

import com.example.liveclass.dto.response.EnrollmentResponse;
import com.example.liveclass.dto.response.PaginatedResponse;
import com.example.liveclass.service.EnrollmentService;
import com.example.liveclass.utils.AuthorizationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
@Tag(name = "수강신청", description = "📝 수강 신청 - 신청, 확정, 취소")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @Operation(summary = "수강 신청 ➕", description = "강의에 수강 신청합니다. **학생(Student)만 가능합니다.**")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "신청 성공 - enrollmentId 반환"),
            @ApiResponse(responseCode = "401", description = "권한 없음 (학생만 가능)"),
            @ApiResponse(responseCode = "409", description = "중복 신청 또는 정원 초과")
    })
    public ResponseEntity<EnrollmentResponse> enrollCourse(
            @Parameter(description = "강의 ID", required = true) @RequestParam String courseId,
            @Parameter(description = "학생 ID", example = "student-1", in = ParameterIn.HEADER)
            @RequestHeader(name = "Authorization") String authorization
    ) {
        String userId = AuthorizationUtils.extractUserId(authorization);  // ✅ Bearer 제거
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enrollCourse(courseId, userId));
    }

    @PatchMapping("/{enrollmentId}/confirm")
    @Operation(summary = "결제 확정 ✅", description = "수강 신청을 확정합니다. `PENDING` → `CONFIRMED`")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "확정 성공"),
            @ApiResponse(responseCode = "401", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "신청을 찾을 수 없음")
    })
    public ResponseEntity<EnrollmentResponse> confirmPayment(
            @PathVariable String enrollmentId,
            @Parameter(description = "학생 ID", example = "student-1", in = ParameterIn.HEADER)
            @RequestHeader(name = "Authorization") String authorization
    ) {
        String userId = AuthorizationUtils.extractUserId(authorization);  // ✅ Bearer 제거
        return ResponseEntity.ok(enrollmentService.confirmPayment(enrollmentId, userId));
    }

    @PatchMapping("/{enrollmentId}/cancel")
    @Operation(summary = "신청 취소 ❌", description = "수강 신청을 취소합니다. **확정 후 7일 이내만 가능합니다.**")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "409", description = "취소 기한 초과")
    })
    public ResponseEntity<EnrollmentResponse> cancelEnrollment(
            @PathVariable String enrollmentId,
            @Parameter(description = "학생 ID", example = "student-1", in = ParameterIn.HEADER)
            @RequestHeader(name = "Authorization") String authorization
    ) {
        String userId = AuthorizationUtils.extractUserId(authorization);  // ✅ Bearer 제거
        return ResponseEntity.ok(enrollmentService.cancelEnrollment(enrollmentId, userId));
    }

    @GetMapping("/{enrollmentId}")
    @Operation(summary = "신청 상세 조회 🔍", description = "특정 신청의 상세 정보를 조회합니다.")
    public ResponseEntity<EnrollmentResponse> getEnrollment(
            @PathVariable String enrollmentId,
            @Parameter(description = "학생 ID", example = "student-1", in = ParameterIn.HEADER)
            @RequestHeader(name = "Authorization") String authorization
    ) {
        String userId = AuthorizationUtils.extractUserId(authorization);  // ✅ Bearer 제거
        return ResponseEntity.ok(enrollmentService.getEnrollment(enrollmentId, userId));
    }

    @GetMapping("/me")
    @Operation(summary = "내 신청 목록 📋", description = "현재 학생의 수강 신청 목록을 조회합니다.")
    public ResponseEntity<PaginatedResponse<EnrollmentResponse>> getMyEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "학생 ID", example = "student-1", in = ParameterIn.HEADER)
            @RequestHeader(name = "Authorization") String authorization
    ) {
        String userId = AuthorizationUtils.extractUserId(authorization);  // ✅ Bearer 제거
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(enrollmentService.getMyEnrollments(userId, pageable));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "강의별 신청 목록 👨‍🎓", description = "특정 강의의 수강생 목록을 조회합니다.")
    public ResponseEntity<PaginatedResponse<EnrollmentResponse>> getEnrollmentsByCourse(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByCourse(courseId, pageable));
    }
}