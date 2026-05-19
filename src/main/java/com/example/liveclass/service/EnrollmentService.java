package com.example.liveclass.service;

import com.example.liveclass.dto.request.EnrollRequest;
import com.example.liveclass.dto.response.EnrollmentResponse;
import com.example.liveclass.dto.response.PaginatedResponse;
import com.example.liveclass.entity.Class;
import com.example.liveclass.entity.ClassStatus;
import com.example.liveclass.entity.Enrollment;
import com.example.liveclass.entity.EnrollmentStatus;
import com.example.liveclass.exception.*;
import com.example.liveclass.repository.ClassRepository;
import com.example.liveclass.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 신청 비즈니스 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;

    private static final long CANCELLATION_DEADLINE_DAYS = 7;  // 7일 제한

    public EnrollmentResponse enroll(EnrollRequest request, String userId) {
        log.info("수강 신청 요청: courseId={}, userId={}", request.getCourseId(), userId);

        Class course = classRepository.findByIdWithLock(request.getCourseId())
                .orElseThrow(() -> new CourseNotFoundException(request.getCourseId()));

        if (course.getStatus() != ClassStatus.OPEN) {
            throw new CourseNotOpenException(course.getId(), course.getStatus().toString());
        }

        if (course.isCapacityFull()) {
            throw new CapacityExceededException(course.getId());
        }

        Optional<Enrollment> existingEnrollment = enrollmentRepository
                .findByCourseIdAndUserId(request.getCourseId(), userId);

        if (existingEnrollment.isPresent()) {
            EnrollmentStatus status = existingEnrollment.get().getStatus();
            if (status == EnrollmentStatus.PENDING || status == EnrollmentStatus.CONFIRMED) {
                throw new DuplicateEnrollmentException(course.getId(), userId);
            }
        }

        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID().toString())
                .courseId(course.getId())
                .userId(userId)
                .status(EnrollmentStatus.PENDING)
                .build();

        return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    public EnrollmentResponse confirmPayment(String enrollmentId, String userId) {
        log.info("결제 확정 요청: enrollmentId={}, userId={}", enrollmentId, userId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        if (!enrollment.getUserId().equals(userId)) {
            throw new UnauthorizedException("이 신청을 변경할 권한이 없습니다");
        }

        if (!enrollment.isPending()) {
            throw new InvalidStateException(enrollment.getStatus().toString(), EnrollmentStatus.CONFIRMED.toString());
        }

        Class course = classRepository.findByIdWithLock(enrollment.getCourseId())
                .orElseThrow(() -> new CourseNotFoundException(enrollment.getCourseId()));

        if (course.isCapacityFull()) {
            throw new CapacityExceededException(course.getId());
        }

        enrollment.confirm();
        Enrollment updated = enrollmentRepository.save(enrollment);

        course.setCurrentEnrollment(course.getCurrentEnrollment() + 1);
        classRepository.save(course);

        return EnrollmentResponse.from(updated);
    }

    public EnrollmentResponse cancelEnrollment(String enrollmentId, String userId) {
        log.info("신청 취소 요청: enrollmentId={}, userId={}", enrollmentId, userId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        if (!enrollment.getUserId().equals(userId)) {
            throw new UnauthorizedException("이 신청을 변경할 권한이 없습니다");
        }

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new InvalidStateException(EnrollmentStatus.CANCELLED.toString(), EnrollmentStatus.CANCELLED.toString());
        }

        if (enrollment.isConfirmed()) {
            checkCancellationDeadline(enrollment);
            
            Class course = classRepository.findById(enrollment.getCourseId())
                    .orElseThrow(() -> new CourseNotFoundException(enrollment.getCourseId()));
            course.setCurrentEnrollment(Math.max(0, course.getCurrentEnrollment() - 1));
            classRepository.save(course);
        }

        enrollment.cancel();
        return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    private void checkCancellationDeadline(Enrollment enrollment) {
        LocalDateTime confirmedAt = enrollment.getConfirmedAt();
        LocalDateTime now = LocalDateTime.now();
        
        if (confirmedAt != null) {
            long daysPassed = ChronoUnit.DAYS.between(confirmedAt, now);
            if (daysPassed > CANCELLATION_DEADLINE_DAYS) {
                // ⭐ 생성자 인자 2개를 맞추기 위해 현재 상태와 타겟 상태를 넘겨줌
                throw new InvalidStateException(enrollment.getStatus().toString(), "EXPIRED_CANCEL");
            }
        }
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<EnrollmentResponse> getMyEnrollments(String userId, List<EnrollmentStatus> statuses, Pageable pageable) {
        Page<Enrollment> page = (statuses == null || statuses.isEmpty()) 
            ? enrollmentRepository.findByUserId(userId, pageable)
            : enrollmentRepository.findByUserIdAndStatusIn(userId, statuses, pageable);
        return PaginatedResponse.from(page.map(EnrollmentResponse::from));
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollment(String enrollmentId, String userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        if (!enrollment.getUserId().equals(userId)) {
            throw new UnauthorizedException("이 신청을 조회할 권한이 없습니다");
        }

        return EnrollmentResponse.from(enrollment);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<EnrollmentResponse> getEnrollmentsByCourse(String courseId, Pageable pageable) {
        return PaginatedResponse.from(enrollmentRepository.findByCourseId(courseId, pageable).map(EnrollmentResponse::from));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getConfirmedEnrollments(String courseId) {
        return enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.CONFIRMED).stream()
                .map(EnrollmentResponse::from)
                .toList();
    }
}
