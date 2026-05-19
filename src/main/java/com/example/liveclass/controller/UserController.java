package com.example.liveclass.controller;

import com.example.liveclass.dto.request.CreateCreatorRequest;
import com.example.liveclass.dto.request.CreateStudentRequest;
import com.example.liveclass.dto.response.CreatorRegistrationResponse;
import com.example.liveclass.dto.response.StudentRegistrationResponse;
import com.example.liveclass.service.CreatorService;
import com.example.liveclass.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 관리 API
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "👤 사용자 관리 - 강사/학생 회원가입")
public class UserController {

    private final CreatorService creatorService;
    private final StudentService studentService;

    /**
     * 강사 회원가입
     */
    @PostMapping("/creators")
    @Operation(
            summary = "강사 회원가입 ➕",
            description = """
                    새로운 강사를 등록합니다.
                    
                    **응답에서 `creatorId`를 저장하세요!**
                    강의 생성 시 Authorization 헤더에 이 값을 사용합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "강사 등록 성공",
                    content = @Content(schema = @Schema(implementation = CreatorRegistrationResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 데이터 오류"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 ID 또는 이메일")
    })
    public ResponseEntity<CreatorRegistrationResponse> createCreator(
            @Valid @RequestBody CreateCreatorRequest request) {
        log.info("강사 회원가입 요청: {}", request.getId());
        CreatorRegistrationResponse response = creatorService.createCreatorWithResponse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 학생 회원가입
     */
    @PostMapping("/students")
    @Operation(
            summary = "학생 회원가입 ➕",
            description = """
                    새로운 학생을 등록합니다.
                    
                    **응답에서 `studentId`를 저장하세요!**
                    수강 신청 시 Authorization 헤더에 이 값을 사용합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "학생 등록 성공",
                    content = @Content(schema = @Schema(implementation = StudentRegistrationResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 데이터 오류"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 ID 또는 이메일")
    })
    public ResponseEntity<StudentRegistrationResponse> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {
        log.info("학생 회원가입 요청: {}", request.getId());
        StudentRegistrationResponse response = studentService.createStudentWithResponse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}