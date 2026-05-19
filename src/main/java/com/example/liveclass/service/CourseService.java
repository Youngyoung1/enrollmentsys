package com.example.liveclass.service;

import com.example.liveclass.dto.request.CreateCourseRequest;
import com.example.liveclass.dto.response.CourseResponse;
import com.example.liveclass.dto.response.PaginatedResponse;
import com.example.liveclass.entity.Course;
import com.example.liveclass.entity.Course.CourseStatus;
import com.example.liveclass.entity.Creator;
import com.example.liveclass.entity.User;
import com.example.liveclass.exception.CourseNotFoundException;
import com.example.liveclass.exception.UnauthorizedException;
import com.example.liveclass.repository.CourseRepository;
import com.example.liveclass.repository.CreatorRepository;
import com.example.liveclass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 강의 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final CreatorRepository creatorRepository;
    private final UserRepository userRepository;

    /**
     * 강의 생성 (Creator만 가능)
     */
    public CourseResponse createCourse(CreateCourseRequest request, String creatorId) {
        // ✅ User 조회 및 role 검증 (CREATOR만 강의 생성 가능)
        User user = userRepository.findById(creatorId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다: " + creatorId));

        if (!user.getRole().equals(User.UserRole.CREATOR)) {
            throw new UnauthorizedException("강사만 강의를 생성할 수 있습니다");
        }

        // Creator 조회
        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new UnauthorizedException("강사를 찾을 수 없습니다: " + creatorId));

        Course course = Course.builder()
                .id(UUID.randomUUID().toString())
                .creator(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .maxCapacity(request.getMaxCapacity())
                .currentEnrollment(0)
                .status(CourseStatus.DRAFT)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Course saved = courseRepository.save(course);
        return CourseResponse.from(saved);
    }

    /**
     * 강의 상태 변경 (Creator만, 본인 강의만)
     */
    public CourseResponse updateCourseStatus(String courseId, CourseStatus status, String creatorId) {
        // ✅ User 조회 및 role 검증
        User user = userRepository.findById(creatorId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다: " + creatorId));

        if (!user.getRole().equals(User.UserRole.CREATOR)) {
            throw new UnauthorizedException("강사만 강의 상태를 변경할 수 있습니다");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("강의를 찾을 수 없습니다: " + courseId));

        // 권한 확인 (강사만 변경 가능)
        if (!course.getCreator().getId().equals(creatorId)) {
            throw new UnauthorizedException("강의 생성자만 상태를 변경할 수 있습니다");
        }

        course.setStatus(status);
        course.setUpdatedAt(LocalDateTime.now());

        Course updated = courseRepository.save(course);
        return CourseResponse.from(updated);
    }

    /**
     * 강의 목록 조회 (상태 필터링 지원)
     */
    public PaginatedResponse<CourseResponse> getCourses(CourseStatus status, Pageable pageable) {
        Page<Course> page;

        if (status != null) {
            page = courseRepository.findByStatus(status, pageable);
        } else {
            page = courseRepository.findAll(pageable);
        }

        return PaginatedResponse.from(
                page.map(CourseResponse::from),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * 강의 상세 조회
     */
    public CourseResponse getCourse(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("강의를 찾을 수 없습니다: " + courseId));

        return CourseResponse.from(course);
    }

    /**
     * 내 강의 목록 (강사가 만든 강의들)
     */
    public PaginatedResponse<CourseResponse> getMyCourses(String creatorId, Pageable pageable) {
        // ✅ User 조회 및 role 검증
        User user = userRepository.findById(creatorId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다: " + creatorId));

        if (!user.getRole().equals(User.UserRole.CREATOR)) {
            throw new UnauthorizedException("강사만 자신의 강의 목록을 조회할 수 있습니다");
        }

        Page<Course> page = courseRepository.findByCreatorId(creatorId, pageable);

        return PaginatedResponse.from(
                page.map(CourseResponse::from),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * 강의 정원 증가
     */
    public void increaseCourseCapacity(String courseId, int count) {
        Course course = courseRepository.findByIdWithLock(courseId)
                .orElseThrow(() -> new CourseNotFoundException("강의를 찾을 수 없습니다: " + courseId));

        if (course.getCurrentEnrollment() + count > course.getMaxCapacity()) {
            throw new IllegalArgumentException("강의 정원을 초과합니다");
        }

        course.setCurrentEnrollment(course.getCurrentEnrollment() + count);
        courseRepository.save(course);
    }

    /**
     * 강의 정원 감소
     */
    public void decreaseCourseCapacity(String courseId, int count) {
        Course course = courseRepository.findByIdWithLock(courseId)
                .orElseThrow(() -> new CourseNotFoundException("강의를 찾을 수 없습니다: " + courseId));

        int newEnrollment = course.getCurrentEnrollment() - count;
        if (newEnrollment < 0) {
            newEnrollment = 0;
        }

        course.setCurrentEnrollment(newEnrollment);
        courseRepository.save(course);
    }
}