package com.example.liveclass.service;

import com.example.liveclass.dto.response.EnrollmentResponse;
import com.example.liveclass.dto.response.PaginatedResponse;
import com.example.liveclass.entity.Course;
import com.example.liveclass.entity.Enrollment;
import com.example.liveclass.entity.Enrollment.EnrollmentStatus;
import com.example.liveclass.entity.Student;
import com.example.liveclass.exception.*;
import com.example.liveclass.repository.CourseRepository;
import com.example.liveclass.repository.EnrollmentRepository;
import com.example.liveclass.repository.StudentRepository;
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
    private final StudentRepository studentRepository;
    private final CourseService courseService;

    private static final int CANCELLATION_PERIOD_DAYS = 7;

    /**
     * 수강 신청
     */
    public EnrollmentResponse enrollCourse(String courseId, String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new UnauthorizedException("학생을 찾을 수 없습니다: " + studentId));

        Course course = courseRepository.findByIdWithLock(courseId)
                .orElseThrow(() -> new CourseNotFoundException("강의를 찾을 수 없습니다: " + courseId));

        // 강의 상태 확인
        if (!course.getStatus().name().equals("OPEN")) {
            throw new CourseNotOpenException("강의가 공개 중이 아닙니다");
        }

        // 정원 확인
        if (course.getCurrentEnrollment() >= course.getMaxCapacity()) {
            throw new CapacityExceededException("강의 정원이 가득 찼습니다");
        }

        // 중복 신청 확인
        enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .ifPresent(e -> {
                    if (e.getStatus() != EnrollmentStatus.CANCELLED) {
                        throw new DuplicateEnrollmentException("이미 신청한 강의입니다");
                    }
                });

        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID().toString())
                .student(student)
                .course(course)
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        
        // 강의 인원 증가
        courseService.increaseCourseCapacity(courseId, 1);

        return EnrollmentResponse.from(saved);
    }

    /**
     * 결제 확정 (PENDING → CONFIRMED)
     */
    public EnrollmentResponse confirmPayment(String enrollmentId, String studentId) {
        Enrollment enrollment = enrollmentRepository.findByIdWithLock(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("신청을 찾을 수 없습니다: " + enrollmentId));

        // 권한 확인
        if (!enrollment.getStudent().getId().equals(studentId)) {
            throw new UnauthorizedException("본인의 신청만 확정할 수 있습니다");
        }

        // 상태 확인
        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new InvalidStateException("신청 상태가 올바르지 않습니다");
        }

        // 강의 정원 재확인
        Course course = enrollment.getCourse();
        if (course.getCurrentEnrollment() > course.getMaxCapacity()) {
            throw new CapacityExceededException("강의 정원이 초과되었습니다");
        }

        enrollment.setStatus(EnrollmentStatus.CONFIRMED);
        enrollment.setConfirmedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());

        Enrollment updated = enrollmentRepository.save(enrollment);
        return EnrollmentResponse.from(updated);
    }

    /**
     * 신청 취소 (CONFIRMED → CANCELLED)
     */
    public EnrollmentResponse cancelEnrollment(String enrollmentId, String studentId) {
        Enrollment enrollment = enrollmentRepository.findByIdWithLock(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("신청을 찾을 수 없습니다: " + enrollmentId));

        // 권한 확인
        if (!enrollment.getStudent().getId().equals(studentId)) {
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

        // 강의 인원 감소
        courseService.decreaseCourseCapacity(enrollment.getCourse().getId(), 1);

        return EnrollmentResponse.from(updated);
    }

    /**
     * 신청 상세 조회
     */
    public EnrollmentResponse getEnrollment(String enrollmentId, String studentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("신청을 찾을 수 없습니다: " + enrollmentId));

        // 권한 확인
        if (!enrollment.getStudent().getId().equals(studentId)) {
            throw new UnauthorizedException("본인의 신청만 조회할 수 있습니다");
        }

        return EnrollmentResponse.from(enrollment);
    }

    /**
     * 내 신청 목록 조회
     */
    public PaginatedResponse<EnrollmentResponse> getMyEnrollments(String studentId, Pageable pageable) {
        Page<Enrollment> page = enrollmentRepository.findByStudentId(studentId, pageable);

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
     * 강의별 수강생 목록 조회 (강사만)
     */
    public PaginatedResponse<EnrollmentResponse> getEnrollmentsByCourse(String courseId, String creatorId, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("강의를 찾을 수 없습니다: " + courseId));

        // 권한 확인 (강사만)
        if (!course.getCreator().getId().equals(creatorId)) {
            throw new UnauthorizedException("강의 생성자만 조회 가능합니다");
        }

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
}
