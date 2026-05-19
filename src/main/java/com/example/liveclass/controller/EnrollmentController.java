package com.example.liveclass.controller;

import com.example.liveclass.dto.request.EnrollRequest;
import com.example.liveclass.dto.response.EnrollmentResponse;
import com.example.liveclass.dto.response.PaginatedResponse;
import com.example.liveclass.entity.EnrollmentStatus;
import com.example.liveclass.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 수강 신청 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * 수강 신청
     */
    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(
            @RequestHeader("Authorization") String userId,
            @Valid @RequestBody EnrollRequest request) {
        log.info("수강 신청 요청: userId={}, courseId={}", userId, request.getCourseId());
        EnrollmentResponse response = enrollmentService.enroll(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 결제 확정 (신청 수락)
     */
    @PatchMapping("/{enrollmentId}/confirm")
    public ResponseEntity<EnrollmentResponse> confirmPayment(
            @PathVariable String enrollmentId,
            @RequestHeader("Authorization") String userId) {
        log.info("결제 확정 요청: enrollmentId={}, userId={}", enrollmentId, userId);
        EnrollmentResponse response = enrollmentService.confirmPayment(enrollmentId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 신청 취소
     */
    @PatchMapping("/{enrollmentId}/cancel")
    public ResponseEntity<EnrollmentResponse> cancelEnrollment(
            @PathVariable String enrollmentId,
            @RequestHeader("Authorization") String userId) {
        log.info("신청 취소 요청: enrollmentId={}, userId={}", enrollmentId, userId);
        EnrollmentResponse response = enrollmentService.cancelEnrollment(enrollmentId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 수강 신청 목록 조회
     */
    @GetMapping("/my")
    public ResponseEntity<PaginatedResponse<EnrollmentResponse>> getMyEnrollments(
            @RequestHeader("Authorization") String userId,
            @RequestParam(required = false) List<EnrollmentStatus> statuses,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("내 신청 목록 조회: userId={}, statuses={}", userId, statuses);
        Pageable pageable = PageRequest.of(page, size);
        PaginatedResponse<EnrollmentResponse> response = enrollmentService.getMyEnrollments(userId, statuses, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 신청 상세 조회
     */
    @GetMapping("/{enrollmentId}")
    public ResponseEntity<EnrollmentResponse> getEnrollment(
            @PathVariable String enrollmentId,
            @RequestHeader("Authorization") String userId) {
        log.info("신청 상세 조회: enrollmentId={}, userId={}", enrollmentId, userId);
        EnrollmentResponse response = enrollmentService.getEnrollment(enrollmentId, userId);
        return ResponseEntity.ok(response);
    }
}
