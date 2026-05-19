package com.example.liveclass;

import com.example.liveclass.dto.request.EnrollRequest;
import com.example.liveclass.entity.CourseStatus;
import com.example.liveclass.entity.Enrollment;
import com.example.liveclass.entity.EnrollmentStatus;
import com.example.liveclass.exception.*;
import com.example.liveclass.repository.CourserRepository;
import com.example.liveclass.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 신청 서비스 유닛 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService 테스트")
public class EnrollmentServiceTest {

    @Mock
    private CourserRepository courserRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private String courseId;
    private String userId;
    private String enrollmentId;
    private Class mockCourse;
    private Enrollment mockEnrollment;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID().toString();
        userId = "student-1";
        enrollmentId = UUID.randomUUID().toString();

        // Mock 강의 설정
        mockCourse = Class.builder()
                .id(courseId)
                .creatorId("creator-1")
                .title("테스트 강의")
                .description("테스트")
                .price(50000)
                .maxCapacity(30)
                .currentEnrollment(0)
                .status(CourseStatus.OPEN)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .build();

        // Mock 신청 설정
        mockEnrollment = Enrollment.builder()
                .id(enrollmentId)
                .courseId(courseId)
                .userId(userId)
                .status(EnrollmentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("정상 신청 - 성공")
    void testEnrollSuccess() {
        // Given
        EnrollRequest request = new EnrollRequest();
        request.setCourseId(courseId);

        when(courserRepository.findByIdWithLock(courseId))
                .thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.findByCourseIdAndUserId(courseId, userId))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenReturn(mockEnrollment);

        // When
        EnrollmentResponse response = enrollmentService.enroll(request, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(enrollmentId);
        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(response.getUserId()).isEqualTo(userId);

        verify(courserRepository, times(1)).findByIdWithLock(courseId);
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("강의 미존재 - 실패")
    void testEnrollFailedCourseNotFound() {
        // Given
        EnrollRequest request = new EnrollRequest();
        request.setCourseId(courseId);

        when(courserRepository.findByIdWithLock(courseId))
                .thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> enrollmentService.enroll(request, userId))
                .isInstanceOf(CourseNotFoundException.class);

        verify(courserRepository, times(1)).findByIdWithLock(courseId);
    }

    @Test
    @DisplayName("강의 상태가 OPEN이 아님 - 실패")
    void testEnrollFailedCourseNotOpen() {
        // Given
        EnrollRequest request = new EnrollRequest();
        request.setCourseId(courseId);

        mockCourse.setStatus(CourseStatus.DRAFT);
        when(courserRepository.findByIdWithLock(courseId))
                .thenReturn(Optional.of(mockCourse));

        // When, Then
        assertThatThrownBy(() -> enrollmentService.enroll(request, userId))
                .isInstanceOf(CourseNotOpenException.class);
    }

    @Test
    @DisplayName("정원 초과 - 실패")
    void testEnrollFailedCapacityExceeded() {
        // Given
        EnrollRequest request = new EnrollRequest();
        request.setCourseId(courseId);

        mockCourse.setCurrentEnrollment(mockCourse.getMaxCapacity());
        when(courserRepository.findByIdWithLock(courseId))
                .thenReturn(Optional.of(mockCourse));

        // When, Then
        assertThatThrownBy(() -> enrollmentService.enroll(request, userId))
                .isInstanceOf(CapacityExceededException.class);
    }

    @Test
    @DisplayName("중복 신청 - 실패")
    void testEnrollFailedDuplicate() {
        // Given
        EnrollRequest request = new EnrollRequest();
        request.setCourseId(courseId);

        when(courserRepository.findByIdWithLock(courseId))
                .thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.findByCourseIdAndUserId(courseId, userId))
                .thenReturn(Optional.of(mockEnrollment));

        // When, Then
        assertThatThrownBy(() -> enrollmentService.enroll(request, userId))
                .isInstanceOf(DuplicateEnrollmentException.class);
    }

    @Test
    @DisplayName("결제 확정 - 성공")
    void testConfirmPaymentSuccess() {
        // Given
        when(enrollmentRepository.findById(enrollmentId))
                .thenReturn(Optional.of(mockEnrollment));
        when(courserRepository.findByIdWithLock(courseId))
                .thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenReturn(mockEnrollment);

        // When
        EnrollmentResponse response = enrollmentService.confirmPayment(enrollmentId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(enrollmentId);

        verify(enrollmentRepository, times(2)).findById(enrollmentId);
        verify(courserRepository, times(1)).findByIdWithLock(courseId);
    }

    @Test
    @DisplayName("결제 확정 - 신청 미존재")
    void testConfirmPaymentEnrollmentNotFound() {
        // Given
        when(enrollmentRepository.findById(enrollmentId))
                .thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> enrollmentService.confirmPayment(enrollmentId, userId))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }

    @Test
    @DisplayName("신청 취소 - 성공")
    void testCancelEnrollmentSuccess() {
        // Given
        mockEnrollment.setStatus(EnrollmentStatus.PENDING);
        when(enrollmentRepository.findById(enrollmentId))
                .thenReturn(Optional.of(mockEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenReturn(mockEnrollment);

        // When
        EnrollmentResponse response = enrollmentService.cancelEnrollment(enrollmentId, userId);

        // Then
        assertThat(response).isNotNull();
        verify(enrollmentRepository, times(1)).findById(enrollmentId);
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("신청 취소 - 이미 취소됨")
    void testCancelEnrollmentAlreadyCancelled() {
        // Given
        mockEnrollment.setStatus(EnrollmentStatus.CANCELLED);
        when(enrollmentRepository.findById(enrollmentId))
                .thenReturn(Optional.of(mockEnrollment));

        // When, Then
        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(enrollmentId, userId))
                .isInstanceOf(InvalidStateException.class);
    }
}
