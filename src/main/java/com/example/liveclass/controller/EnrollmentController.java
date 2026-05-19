package com.example.liveclass.controller;

import com.example.liveclass.dto.response.EnrollmentResponse;
import com.example.liveclass.dto.response.PaginatedResponse;
import com.example.liveclass.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 수강 신청 관련 API
 */
@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
@Tag(name = "📝 수강 신청", description = "수강 신청, 확정, 취소 관련 API")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * 수강 신청
     */
    @PostMapping
    @Operation(summary = "수강 신청 ➕", description = "강의에 신청합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "신청 성공"),
            @ApiResponse(responseCode = "400", description = "요청 데이터 오류"),
            @ApiResponse(responseCode = "409", description = "중복 신청 또는 정원 초과")
    })
    public ResponseEntity<EnrollmentResponse> enrollCourse(
            @Parameter(description = "강의 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String courseId,
            @Parameter(description = "학생 ID", example = "student-1")
            @RequestHeader(value = "Authorization", required = false) String studentId
    ) {
        if (studentId == null || studentId.trim().isEmpty()) {
            studentId = "anonymous";
        }
        EnrollmentResponse response = enrollmentService.enrollCourse(courseId, studentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 결제 확정
     */
    @PatchMapping("/{enrollmentId}/confirm")
    @Operation(summary = "결제 확정 ✅", description = "신청한 강의의 결제를 확정합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "확정 성공"),
            @ApiResponse(responseCode = "401", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "신청을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "상태 오류")
    })
    public ResponseEntity<EnrollmentResponse> confirmPayment(
            @Parameter(description = "신청 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String enrollmentId,
            @Parameter(description = "학생 ID", example = "student-1")
            @RequestHeader(value = "Authorization", required = false) String studentId
    ) {
        if (studentId == null || studentId.trim().isEmpty()) {
            studentId = "anonymous";
        }
        EnrollmentResponse response = enrollmentService.confirmPayment(enrollmentId, studentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 신청 취소
     */
    @PatchMapping("/{enrollmentId}/cancel")
    @Operation(summary = "신청 취소 ❌", description = "신청한 강의를 취소합니다 (7일 이내)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "401", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "신청을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "상태 오류 또는 취소 기한 초과")
    })
    public ResponseEntity<EnrollmentResponse> cancelEnrollment(
            @Parameter(description = "신청 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String enrollmentId,
            @Parameter(description = "학생 ID", example = "student-1")
            @RequestHeader(value = "Authorization", required = false) String studentId
    ) {
        if (studentId == null || studentId.trim().isEmpty()) {
            studentId = "anonymous";
        }
        EnrollmentResponse response = enrollmentService.cancelEnrollment(enrollmentId, studentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 신청 상세 조회
     */
    @GetMapping("/{enrollmentId}")
    @Operation(summary = "신청 상세 조회 🔍", description = "특정 신청의 상세 정보를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "신청을 찾을 수 없음")
    })
    public ResponseEntity<EnrollmentResponse> getEnrollment(
            @Parameter(description = "신청 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String enrollmentId,
            @Parameter(description = "학생 ID", example = "student-1")
            @RequestHeader(value = "Authorization", required = false) String studentId
    ) {
        if (studentId == null || studentId.trim().isEmpty()) {
            studentId = "anonymous";
        }
        EnrollmentResponse response = enrollmentService.getEnrollment(enrollmentId, studentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 신청 목록
     */
    @GetMapping("/me")
    @Operation(summary = "내 신청 목록 📋", description = "현재 사용자의 수강 신청 목록을 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<PaginatedResponse<EnrollmentResponse>> getMyEnrollments(
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "학생 ID", example = "student-1")
            @RequestHeader(value = "Authorization", required = false) String studentId
    ) {
        if (studentId == null || studentId.trim().isEmpty()) {
            studentId = "anonymous";
        }
        Pageable pageable = PageRequest.of(page, size);
        PaginatedResponse<EnrollmentResponse> response = enrollmentService.getMyEnrollments(studentId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 강의별 수강생 목록 (강사만)
     */
    @GetMapping("/course/{courseId}")
    @Operation(summary = "강의별 수강생 목록 👨‍🎓", description = "특정 강의의 수강생 목록을 조회합니다 (강사만)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "권한 없음 (강사만)"),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
    })
    public ResponseEntity<PaginatedResponse<EnrollmentResponse>> getEnrollmentsByCourse(
            @Parameter(description = "강의 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "강사 ID", example = "creator-1")
            @RequestHeader(value = "Authorization", required = false) String creatorId
    ) {
        if (creatorId == null || creatorId.trim().isEmpty()) {
            creatorId = "anonymous";
        }
        Pageable pageable = PageRequest.of(page, size);
        PaginatedResponse<EnrollmentResponse> response = enrollmentService.getEnrollmentsByCourse(courseId, creatorId, pageable);
        return ResponseEntity.ok(response);
    }
}
