package com.example.liveclass.service;

import com.example.liveclass.dto.request.CreateClassRequest;
import com.example.liveclass.dto.response.ClassResponse;
import com.example.liveclass.dto.response.PaginatedResponse;
import com.example.liveclass.entity.Class;
import com.example.liveclass.entity.ClassStatus;
import com.example.liveclass.exception.CourseNotFoundException;
import com.example.liveclass.exception.InvalidStateException;
import com.example.liveclass.exception.UnauthorizedException;
import com.example.liveclass.repository.ClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 강의 비즈니스 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClassService {

    private final ClassRepository classRepository;

    /**
     * 강의 생성
     */
    public ClassResponse createClass(CreateClassRequest request, String creatorId) {
        log.info("강의 생성: title={}, creatorId={}", request.getTitle(), creatorId);

        Class course = Class.builder()
                .id(UUID.randomUUID().toString())
                .creatorId(creatorId)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .maxCapacity(request.getMaxCapacity())
                .currentEnrollment(0)
                .status(ClassStatus.DRAFT)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        Class saved = classRepository.save(course);
        log.info("강의 생성 완료: courseId={}", saved.getId());

        return ClassResponse.from(saved);
    }

    /**
     * 강의 상태 변경
     */
    public ClassResponse updateClassStatus(String courseId, ClassStatus targetStatus, String userId) {
        log.info("강의 상태 변경: courseId={}, targetStatus={}", courseId, targetStatus);

        Class course = classRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        // 권한 확인
        if (!course.getCreatorId().equals(userId)) {
            throw new UnauthorizedException("이 강의를 수정할 권한이 없습니다");
        }

        // 상태 전이 검증
        validateStatusTransition(course.getStatus(), targetStatus);

        course.setStatus(targetStatus);
        Class updated = classRepository.save(course);

        log.info("강의 상태 변경 완료: courseId={}, status={}", courseId, targetStatus);
        return ClassResponse.from(updated);
    }

    /**
     * 전체 강의 목록 조회
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ClassResponse> getAllClasses(Pageable pageable) {
        log.info("전체 강의 목록 조회: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Class> page = classRepository.findAll(pageable);
        Page<ClassResponse> responses = page.map(ClassResponse::from);

        return PaginatedResponse.from(responses);
    }

    /**
     * 상태별 강의 목록 조회
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ClassResponse> getClassesByStatus(ClassStatus status, Pageable pageable) {
        log.info("상태별 강의 목록 조회: status={}, page={}", status, pageable.getPageNumber());

        Page<Class> page = classRepository.findByStatus(status, pageable);
        Page<ClassResponse> responses = page.map(ClassResponse::from);

        return PaginatedResponse.from(responses);
    }

    /**
     * 강의 상세 조회
     */
    @Transactional(readOnly = true)
    public ClassResponse getClassById(String courseId) {
        log.info("강의 상세 조회: courseId={}", courseId);

        Class course = classRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        return ClassResponse.from(course);
    }

    /**
     * 크리에이터의 강의 목록 조회
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ClassResponse> getMyClasses(String creatorId, Pageable pageable) {
        log.info("크리에이터 강의 목록 조회: creatorId={}", creatorId);

        Page<Class> page = classRepository.findByCreatorId(creatorId, pageable);
        Page<ClassResponse> responses = page.map(ClassResponse::from);

        return PaginatedResponse.from(responses);
    }

    /**
     * 상태 전이 검증
     * DRAFT → OPEN → CLOSED만 허용
     */
    private void validateStatusTransition(ClassStatus currentStatus, ClassStatus targetStatus) {
        if (currentStatus == ClassStatus.DRAFT && targetStatus == ClassStatus.OPEN) {
            return;  // 허용
        }

        if (currentStatus == ClassStatus.OPEN && targetStatus == ClassStatus.CLOSED) {
            return;  // 허용
        }

        throw new InvalidStateException(currentStatus.toString(), targetStatus.toString());
    }
}