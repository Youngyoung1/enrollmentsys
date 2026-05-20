package com.example.liveclass.controller;

import com.example.liveclass.dto.request.CreateCourseRequest;
import com.example.liveclass.dto.request.UpdateCourseStatusRequest;
import com.example.liveclass.dto.response.CourseResponse;
import com.example.liveclass.dto.response.PaginatedResponse;
import com.example.liveclass.entity.Course.CourseStatus;
import com.example.liveclass.service.CourseService;
import com.example.liveclass.utils.AuthorizationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "강의관리", description = "🎬 강의 관리 - 강의 생성, 상태 변경, 조회")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @SecurityRequirement(name = "ApiKeyAuth")
    @Operation(summary = "강의 생성 ➕", description = "새 강의를 생성합니다. **강사(Creator)만 가능합니다.**")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "강의 생성 성공"),
            @ApiResponse(responseCode = "401", description = "권한 없음 (강사만 가능)")
    })
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @Parameter(description = "강사 ID", example = "creator-1", in = ParameterIn.HEADER)
            @RequestHeader(name = "Authorization") String authorization
    ) {
        String creatorId = AuthorizationUtils.extractUserId(authorization);
        CourseResponse response = courseService.createCourse(request, creatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{courseId}/status")
    @SecurityRequirement(name = "ApiKeyAuth")
    @Operation(summary = "강의 상태 변경 🔄", description = "강의 상태를 변경합니다. **본인 강의만 가능합니다.**")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "401", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "강의를 찾을 수 없음")
    })
    public ResponseEntity<CourseResponse> updateCourseStatus(
            @PathVariable String courseId,
            @Valid @RequestBody UpdateCourseStatusRequest request,
            @Parameter(description = "강사 ID", example = "creator-1", in = ParameterIn.HEADER)
            @RequestHeader(name = "Authorization") String authorization
    ) {
        String creatorId = AuthorizationUtils.extractUserId(authorization);
        CourseResponse response = courseService.updateCourseStatus(courseId, request.getStatus(), creatorId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "강의 목록 조회 📋", description = "강의 목록을 조회합니다. **누구나 조회 가능합니다.**")
    public ResponseEntity<PaginatedResponse<CourseResponse>> getCourses(
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(name = "Authorization", required = false, defaultValue = "") String authorization
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(courseService.getCourses(status, pageable));
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "강의 상세 조회 🔍", description = "특정 강의의 상세 정보를 조회합니다.")
    public ResponseEntity<CourseResponse> getCourse(
            @PathVariable String courseId,
            @RequestHeader(name = "Authorization", required = false, defaultValue = "") String authorization
    ) {
        return ResponseEntity.ok(courseService.getCourse(courseId));
    }

    @GetMapping("/creator/my-courses")
    @SecurityRequirement(name = "ApiKeyAuth")
    @Operation(summary = "내 강의 목록 👨‍🏫", description = "강사가 생성한 강의 목록을 조회합니다.")
    public ResponseEntity<PaginatedResponse<CourseResponse>> getMyCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "강사 ID", example = "creator-1", in = ParameterIn.HEADER)
            @RequestHeader(name = "Authorization") String authorization
    ) {
        String creatorId = AuthorizationUtils.extractUserId(authorization);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(courseService.getMyCourses(creatorId, pageable));
    }
}