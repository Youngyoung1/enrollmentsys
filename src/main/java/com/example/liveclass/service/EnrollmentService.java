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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 수강 신청 서비스
 * - PENDING: 정원 내 (결제 대기)
 * - WAITING: 정원 초과 (대기열)
 * - CONFIRMED: 결제 확정
 * - CANCELLED: 취소
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    private static final int CANCELLATION_PERIOD_DAYS = 7;

    /**
     * 수강 신청
     * 정원 내: PENDING, 정원 초과: WAITING (대기열)
     */
    public EnrollmentResponse enrollCourse(String courseId, String userId) {
        log.info("수강 신청: courseId={}, userId={}", courseId, userId);

        // 권한 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다: " + userId));

        if (!user.getRole().equals(User.UserRole.STUDENT)) {
            throw new UnauthorizedException("학생만 신청할 수 있습니다");
        }

        // 강의 조회 (락)
        Course course = courseRepository.findByIdWithLock(courseId)
                .orElseThrow(() -> new CourseNotFoundException("강의를 찾을 수 없습니다: " + courseId));

        if (!course.getStatus().equals(Course.CourseStatus.OPEN)) {
            throw new CourseNotOpenException("강의가 공개 중이 아닙니다");
        }

        // 중복 확인
        enrollmentRepository.findByCourseIdAndUserId(courseId, userId)
                .ifPresent(e -> {
                    if (e.getStatus() != EnrollmentStatus.CANCELLED) {
                        throw new DuplicateEnrollmentException("이미 신청한 강의입니다");
                    }
                });

        // ✅ 현재 활성 신청 수
        Integer activeCount = enrollmentRepository.countActiveByCourseId(courseId);

        // ✅ 다음 순번 (PENDING/WAITING 모두 포함)
        Integer nextPosition = enrollmentRepository.findMaxQueuePosition(courseId) + 1;

        // ✅ 상태 결정: 정원 내면 PENDING, 초과하면 WAITING
        EnrollmentStatus status = (activeCount < course.getMaxCapacity())
                ? EnrollmentStatus.PENDING
                : EnrollmentStatus.WAITING;

        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID().toString())
                .courseId(courseId)
                .userId(userId)
                .queuePosition(nextPosition)
                .status(status)
                .enrolledAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        try {
            Enrollment saved = enrollmentRepository.saveAndFlush(enrollment);
            log.info("신청 완료: position={}, status={}", nextPosition, status);
            return EnrollmentResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEnrollmentException("이미 신청한 강의입니다");
        }
    }

    /**
     * 결제 확정 (PENDING → CONFIRMED)
     */
    public EnrollmentResponse confirmPayment(String enrollmentId, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다: " + userId));

        if (!user.getRole().equals(User.UserRole.STUDENT)) {
            throw new UnauthorizedException("학생만 결제를 확정할 수 있습니다");
        }

        Enrollment enrollment = enrollmentRepository.findByIdWithLock(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("신청을 찾을 수 없습니다: " + enrollmentId));

        if (!enrollment.getUserId().equals(userId)) {
            throw new UnauthorizedException("본인의 신청만 확정할 수 있습니다");
        }

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new InvalidStateException("PENDING 상태만 확정할 수 있습니다. 현재: " + enrollment.getStatus());
        }

        // Course에 락 (정원 체크)
        Course course = courseRepository.findByIdWithLock(enrollment.getCourseId())
                .orElseThrow(() -> new CourseNotFoundException("강의를 찾을 수 없습니다"));

        Integer confirmedCount = enrollmentRepository.countConfirmedByCourseId(enrollment.getCourseId());
        if (confirmedCount >= course.getMaxCapacity()) {
            throw new CapacityExceededException("강의 정원이 초과되었습니다");
        }

        enrollment.setStatus(EnrollmentStatus.CONFIRMED);
        enrollment.setConfirmedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());

        course.setCurrentEnrollment(course.getCurrentEnrollment() + 1);
        courseRepository.save(course);

        Enrollment updated = enrollmentRepository.save(enrollment);
        return EnrollmentResponse.from(updated);
    }

    /**
     * 신청 취소
     * 취소 시 WAITING 중 첫 번째를 PENDING으로 자동 승격
     */
    public EnrollmentResponse cancelEnrollment(String enrollmentId, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다: " + userId));

        if (!user.getRole().equals(User.UserRole.STUDENT)) {
            throw new UnauthorizedException("학생만 취소할 수 있습니다");
        }

        Enrollment enrollment = enrollmentRepository.findByIdWithLock(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("신청을 찾을 수 없습니다: " + enrollmentId));

        if (!enrollment.getUserId().equals(userId)) {
            throw new UnauthorizedException("본인의 신청만 취소할 수 있습니다");
        }

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new InvalidStateException("이미 취소된 신청입니다");
        }

        // 취소 기한 확인 (CONFIRMED만)
        if (enrollment.getStatus() == EnrollmentStatus.CONFIRMED) {
            LocalDateTime deadline = enrollment.getConfirmedAt().plusDays(CANCELLATION_PERIOD_DAYS);
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new CancellationPeriodExceededException("취소 기한이 지났습니다 (7일 이내)");
            }
        }

        boolean wasActive = (enrollment.getStatus() == EnrollmentStatus.PENDING
                || enrollment.getStatus() == EnrollmentStatus.CONFIRMED);

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollment.setCancelledAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());

        // Course의 currentEnrollment 감소 (CONFIRMED인 경우)
        if (enrollment.getStatus() == EnrollmentStatus.CONFIRMED) {
            Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
            if (course != null) {
                course.setCurrentEnrollment(Math.max(0, course.getCurrentEnrollment() - 1));
                courseRepository.save(course);
            }
        }

        Enrollment updated = enrollmentRepository.save(enrollment);

        // ✅ 자리가 났으면 WAITING 첫 번째 → PENDING 승격
        if (wasActive) {
            promoteNextWaiting(enrollment.getCourseId());
        }

        return EnrollmentResponse.from(updated);
    }

    /**
     * WAITING 첫 번째 → PENDING 자동 승격
     */
    private void promoteNextWaiting(String courseId) {
        List<Enrollment> waitingList = enrollmentRepository
                .findFirstWaitingByCourseIdWithLock(courseId);

        if (waitingList.isEmpty()) {
            log.info("대기자 없음: courseId={}", courseId);
            return;
        }

        Enrollment next = waitingList.get(0);
        next.setStatus(EnrollmentStatus.PENDING);
        next.setUpdatedAt(LocalDateTime.now());
        enrollmentRepository.save(next);

        log.info("대기자 자동 승격: userId={}, position={}",
                next.getUserId(), next.getQueuePosition());
    }

    /**
     * 신청 상세 조회
     */
    public EnrollmentResponse getEnrollment(String enrollmentId, String userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("신청을 찾을 수 없습니다: " + enrollmentId));

        if (!enrollment.getUserId().equals(userId)) {
            throw new UnauthorizedException("본인의 신청만 조회할 수 있습니다");
        }

        return EnrollmentResponse.from(enrollment);
    }

    /**
     * 내 신청 목록
     */
    public PaginatedResponse<EnrollmentResponse> getMyEnrollments(String userId, Pageable pageable) {
        Page<Enrollment> page = enrollmentRepository.findByUserId(userId, pageable);
        return PaginatedResponse.from(
                page.map(EnrollmentResponse::from),
                page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast()
        );
    }

    /**
     * 강의별 신청 목록 (강사용)
     */
    public PaginatedResponse<EnrollmentResponse> getEnrollmentsByCourse(String courseId, Pageable pageable) {
        Page<Enrollment> page = enrollmentRepository.findByCourseId(courseId, pageable);
        return PaginatedResponse.from(
                page.map(EnrollmentResponse::from),
                page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast()
        );
    }

    public Integer getConfirmedEnrollmentCount(String courseId) {
        Integer count = enrollmentRepository.countConfirmedByCourseId(courseId);
        return count != null ? count : 0;
    }

    /**
     * 스케줄러: 강의 시작 시 PENDING → CONFIRMED 자동 전환
     * 1분마다 실행
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoConfirmOnCourseStart() {
        LocalDateTime now = LocalDateTime.now();
        log.info("자동 확정 스케줄러 실행: {}", now);

        List<Course> startedCourses = courseRepository
                .findByStatusAndStartDateBefore(Course.CourseStatus.OPEN, now);

        for (Course course : startedCourses) {
            List<Enrollment> pendings = enrollmentRepository
                    .findByCourseIdAndStatus(course.getId(), EnrollmentStatus.PENDING);

            int confirmedCount = 0;
            for (Enrollment enrollment : pendings) {
                Integer current = enrollmentRepository.countConfirmedByCourseId(course.getId());
                if (current >= course.getMaxCapacity()) break;

                enrollment.setStatus(EnrollmentStatus.CONFIRMED);
                enrollment.setConfirmedAt(now);
                enrollment.setUpdatedAt(now);
                enrollmentRepository.save(enrollment);

                course.setCurrentEnrollment(course.getCurrentEnrollment() + 1);
                confirmedCount++;
            }

            if (confirmedCount > 0) {
                courseRepository.save(course);
                log.info("강의 시작 - 자동 확정: courseId={}, count={}",
                        course.getId(), confirmedCount);
            }
        }
    }
}