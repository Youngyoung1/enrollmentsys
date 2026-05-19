package com.example.liveclass.controller;

import com.example.liveclass.dto.request.CreateClassRequest;
import com.example.liveclass.dto.request.UpdateClassStatusRequest;
import com.example.liveclass.dto.response.ClassResponse;
import com.example.liveclass.dto.response.PaginatedResponse;
import com.example.liveclass.entity.ClassStatus;
import com.example.liveclass.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 강의 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    /**
     * 강의 생성
     */
    @PostMapping
    public ResponseEntity<ClassResponse> createClass(
            @RequestHeader("Authorization") String userId,
            @Valid @RequestBody CreateClassRequest request) {
        log.info("강의 생성 요청: creatorId={}", userId);
        ClassResponse response = classService.createClass(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 강의 상태 변경
     */
    @PatchMapping("/{courseId}/status")
    public ResponseEntity<ClassResponse> updateClassStatus(
            @PathVariable String courseId,
            @RequestHeader("Authorization") String userId,
            @Valid @RequestBody UpdateClassStatusRequest request) {
        log.info("강의 상태 변경: courseId={}, status={}", courseId, request.getStatus());
        
        // ⭐ 수정됨: request 객체가 아닌 request.getStatus()를 전달해야 함
        ClassResponse response = classService.updateClassStatus(courseId, request.getStatus(), userId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 강의 목록 조회
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<ClassResponse>> getClasses(
            @RequestHeader("Authorization") String userId,
            @RequestParam(value = "status", required = false) ClassStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("강의 목록 조회: status={}, page={}, size={}", status, page, size);

        Pageable pageable = PageRequest.of(page, size);

        PaginatedResponse<ClassResponse> response;
        if (status != null) {
            response = classService.getClassesByStatus(status, pageable);
        } else {
            response = classService.getAllClasses(pageable);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 강의 상세 조회
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<ClassResponse> getClass(
            @PathVariable String courseId,
            @RequestHeader("Authorization") String userId) {
        log.info("강의 상세 조회: courseId={}", courseId);
        ClassResponse response = classService.getClassById(courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 강의 목록 조회 (크리에이터)
     */
    @GetMapping("/creator/my-classes")
    public ResponseEntity<PaginatedResponse<ClassResponse>> getMyClasses(
            @RequestHeader("Authorization") String creatorId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("크리에이터 강의 목록 조회: creatorId={}", creatorId);

        Pageable pageable = PageRequest.of(page, size);
        PaginatedResponse<ClassResponse> response = classService.getMyClasses(creatorId, pageable);
        return ResponseEntity.ok(response);
    }
}
