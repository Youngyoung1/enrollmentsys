package com.example.liveclass.service;

import com.example.liveclass.dto.response.EnrollmentResponse;
import com.example.liveclass.dto.response.PaginatedResponse;
import com.example.liveclass.entity.Course;
import com.example.liveclass.entity.Enrollment;
import com.example.liveclass.entity.EnrollmentStatus;
import com.example.liveclass.entity.User;
import com.example.liveclass.exception.*;
import com.example.liveclass.repository.CourseRepository;
import com.example.liveclass.repository.EnrollmentRepository;
import com.example.liveclass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 수강 신청 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    private static final int CANCELLATION_PERIOD_DAYS = 7;

    /**
     * 수강 신청 (Student만 가능)
     */
    public EnrollmentResponse enrollCourse(String courseId, String userId) {
        // ✅ User 조회 및 role 검증 (STUDENT만 신청 가능)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다: " + userId));

        if (!user.getRole().equals(User.UserRole.STUDENT)) {
            throw new UnauthorizedException("학생만 강의에 신청할 수 있습니다");
        }

        // 강의 존재 확인
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("강의를 찾을 수 없습니다: " + courseId));

        // 강의 상태 확인 (OPEN 상태만 신청 가능)
        if (!course.getStatus().equals(Course.CourseStatus.OPEN)) {
            throw new CourseNotOpenException("강의가 공개 중이 아닙니다");
        }

        // 정원 확인
        Integer confirmedCount = enrollmentRepository.countConfirmedByCourseId(courseId);
        if (confirmedCount != null && confirmedCount >= course.getMaxCapacity()) {
            throw new CapacityExceededException("강의 정원이 가득 찼습니다");
        }

        // 중복 신청 확인
        enrollmentRepository.findByCourseIdAndUserId(courseId, userId)
                .ifPresent(e -> {
                    if (e.getStatus() != EnrollmentStatus.CANCELLED) {
                        throw new DuplicateEnrollmentException("이미 신청한 강의입니다");
                    }
                });

        // 신청 생성
        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .courseId(courseId)
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        return EnrollmentResponse.from(saved);
    }

    /**
     * 결제 확정 (PENDING → CONFIRMED) - Student만
     */
    public EnrollmentResponse confirmPayment(String enrollmentId, String userId) {
        // ✅ User 조회 및 role 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다: " + userId));

        if (!user.getRole().equals(User.UserRole.STUDENT)) {
            throw new UnauthorizedException("학생만 결제를 확정할 수 있습니다");
        }

        Enrollment enrollment = enrollmentRepository.findByIdWithLock(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("신청을 찾을 수 없습니다: " + enrollmentId));

        // 권한 확인
        if (!enrollment.getUserId().equals(userId)) {
            throw new UnauthorizedException("본인의 신청만 확정할 수 있습니다");
        }

        // 상태 확인
        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new InvalidStateException("신청 상태가 올바르지 않습니다");
        }

        // 강의 정원 재확인
        Course course = courseRepository.findById(enrollment.getCourseId())
                .orElseThrow(() -> new CourseNotFoundException("강의를 찾을 수 없습니다"));

        Integer confirmedCount = enrollmentRepository.countConfirmedByCourseId(enrollment.getCourseId());
        if (confirmedCount != null && confirmedCount >= course.getMaxCapacity()) {
            throw new CapacityExceededException("강의 정원이 초과되었습니다");
        }

        enrollment.setStatus(EnrollmentStatus.CONFIRMED);
        enrollment.setConfirmedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());

        Enrollment updated = enrollmentRepository.save(enrollment);
        return EnrollmentResponse.from(updated);
    }

    /**
     * 신청 취소 (CONFIRMED → CANCELLED) - Student만
     */
    public EnrollmentResponse cancelEnrollment(String enrollmentId, String userId) {
        // ✅ User 조회 및 role 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다: " + userId));

        if (!user.getRole().equals(User.UserRole.STUDENT)) {
            throw new UnauthorizedException("학생만 신청을 취소할 수 있습니다");
        }

        Enrollment enrollment = enrollmentRepository.findByIdWithLock(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("신청을 찾을 수 없습니다: " + enrollmentId));

        // 권한 확인
        if (!enrollment.getUserId().equals(userId)) {
            throw new UnauthorizedException("본인의 신청만 취소할 수 있습니다");
        }

        // 상태 확인
        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new InvalidStateException("이미 취소된 신청입니다");
        }

        // 취소 기한 확인 (CONFIRMED 후 7일 이내만 취소 가능)
        if (enrollment.getStatus() == EnrollmentStatus.CONFIRMED) {
            LocalDateTime deadline = enrollment.getConfirmedAt().plusDays(CANCELLATION_PERIOD_DAYS);
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new CancellationPeriodExceededException("취소 기한이 지났습니다 (7일 이내)");
            }
        }

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollment.setCancelledAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());

        Enrollment updated = enrollmentRepository.save(enrollment);
        return EnrollmentResponse.from(updated);
    }

    /**
     * 신청 상세 조회 - Student만
     */
    public EnrollmentResponse getEnrollment(String enrollmentId, String userId) {
        // ✅ User 조회 및 role 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다: " + userId));

        if (!user.getRole().equals(User.UserRole.STUDENT)) {
            throw new UnauthorizedException("학생만 신청을 조회할 수 있습니다");
        }

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("신청을 찾을 수 없습니다: " + enrollmentId));

        // 권한 확인
        if (!enrollment.getUserId().equals(userId)) {
            throw new UnauthorizedException("본인의 신청만 조회할 수 있습니다");
        }

        return EnrollmentResponse.from(enrollment);
    }

    /**
     * 내 신청 목록 조회 - Student만
     */
    public PaginatedResponse<EnrollmentResponse> getMyEnrollments(String userId, Pageable pageable) {
        // ✅ User 조회 및 role 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다: " + userId));

        if (!user.getRole().equals(User.UserRole.STUDENT)) {
            throw new UnauthorizedException("학생만 자신의 신청 목록을 조회할 수 있습니다");
        }

        Page<Enrollment> page = enrollmentRepository.findByUserId(userId, pageable);

        return PaginatedResponse.from(
                page.map(EnrollmentResponse::from),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * 강의별 신청 목록 조회 (강사용)
     */
    public PaginatedResponse<EnrollmentResponse> getEnrollmentsByCourse(String courseId, Pageable pageable) {
        Page<Enrollment> page = enrollmentRepository.findByCourseId(courseId, pageable);

        return PaginatedResponse.from(
                page.map(EnrollmentResponse::from),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * 강의의 확정된 신청 개수 조회
     */
    public Integer getConfirmedEnrollmentCount(String courseId) {
        Integer count = enrollmentRepository.countConfirmedByCourseId(courseId);
        return count != null ? count : 0;
    }
}