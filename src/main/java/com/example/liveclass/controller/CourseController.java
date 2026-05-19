package com.example.liveclass.controller;

import com.example.liveclass.dto.request.CreateCourseRequest;
import com.example.liveclass.dto.request.UpdateCourseStatusRequest;
import com.example.liveclass.dto.response.CourseResponse;
import com.example.liveclass.dto.response.PaginatedResponse;
import com.example.liveclass.entity.Course.CourseStatus;
import com.example.liveclass.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 강의 관련 API
 */
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "🎬 강의 관리", description = "강의 생성, 상태 변경, 조회 관련 API")
public class CourseController {

    private final CourseService courseService;

    /**
     * 강의 생성
     */
    @PostMapping
    @Operation(summary = "강의 생성 ➕", description = "새 강의를 생성합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "강의 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 데이터 오류")
    })
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @Parameter(description = "생성자 ID", example = "creator-1")
            @RequestHeader(value = "Authorization", required = false) String creatorId
    ) {
        if (creatorId == null || creatorId.trim().isEmpty()) {
            creatorId = "anonymous";
        }
        CourseResponse response = courseService.createCourse(request, creatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 강의 상태 변경
     */
    @PatchMapping("/{courseId}/status")
    @Operation(summary = "강의 상태 변경 🔄", description = "강의의 상태를 변경합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "401", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
    })
    public ResponseEntity<CourseResponse> updateCourseStatus(
            @Parameter(description = "강의 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @Valid @RequestBody UpdateCourseStatusRequest request,
            @Parameter(description = "생성자 ID", example = "creator-1")
            @RequestHeader(value = "Authorization", required = false) String creatorId
    ) {
        if (creatorId == null || creatorId.trim().isEmpty()) {
            creatorId = "anonymous";
        }
        CourseResponse response = courseService.updateCourseStatus(courseId, request.getStatus(), creatorId);
        return ResponseEntity.ok(response);
    }

    /**
     * 강의 목록 조회
     */
    @GetMapping
    @Operation(summary = "강의 목록 조회 📋", description = "강의 목록을 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<PaginatedResponse<CourseResponse>> getCourses(
            @Parameter(description = "강의 상태 (DRAFT, OPEN, CLOSED)", example = "OPEN")
            @RequestParam(required = false) CourseStatus status,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "사용자 ID", example = "student-1")
            @RequestHeader(value = "Authorization", required = false) String userId
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PaginatedResponse<CourseResponse> response = courseService.getCourses(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 강의 상세 조회
     */
    @GetMapping("/{courseId}")
    @Operation(summary = "강의 상세 조회 🔍", description = "특정 강의의 상세 정보를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
    })
    public ResponseEntity<CourseResponse> getCourse(
            @Parameter(description = "강의 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @Parameter(description = "사용자 ID", example = "student-1")
            @RequestHeader(value = "Authorization", required = false) String userId
    ) {
        CourseResponse response = courseService.getCourse(courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 강의 목록
     */
    @GetMapping("/creator/my-courses")
    @Operation(summary = "내 강의 목록 👨‍🏫", description = "현재 사용자가 생성한 강의 목록을 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<PaginatedResponse<CourseResponse>> getMyCourses(
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "생성자 ID", example = "creator-1")
            @RequestHeader(value = "Authorization", required = false) String creatorId
    ) {
        if (creatorId == null || creatorId.trim().isEmpty()) {
            creatorId = "anonymous";
        }
        Pageable pageable = PageRequest.of(page, size);
        PaginatedResponse<CourseResponse> response = courseService.getMyCourses(creatorId, pageable);
        return ResponseEntity.ok(response);
    }
}
