package com.example.liveclass;

import com.example.liveclass.dto.response.EnrollmentResponse;
import com.example.liveclass.entity.Course;
import com.example.liveclass.entity.Course.CourseStatus;
import com.example.liveclass.entity.Enrollment;
import com.example.liveclass.entity.EnrollmentStatus;
import com.example.liveclass.entity.User;
import com.example.liveclass.exception.*;
import com.example.liveclass.repository.CourseRepository;
import com.example.liveclass.repository.EnrollmentRepository;
import com.example.liveclass.repository.UserRepository;
import com.example.liveclass.service.EnrollmentService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * EnrollmentService 유닛 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService 테스트")
class EnrollmentServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private String courseId;
    private String userId;
    private String enrollmentId;
    private Course mockCourse;
    private Enrollment mockEnrollment;
    private User mockUser;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID().toString();
        userId = "student-1";
        enrollmentId = UUID.randomUUID().toString();

        // Mock User (학생)
        mockUser = User.builder()
                .id(userId)
                .name("학생1")
                .email("student1@test.com")
                .role(User.UserRole.STUDENT)
                .createdAt(LocalDateTime.now())
                .build();

        // Mock 강의
        mockCourse = Course.builder()
                .id(courseId)
                .title("테스트 강의")
                .description("테스트")
                .price(50000)
                .maxCapacity(30)
                .currentEnrollment(0)
                .status(CourseStatus.OPEN)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Mock 신청
        mockEnrollment = Enrollment.builder()
                .id(enrollmentId)
                .courseId(courseId)
                .userId(userId)
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("정상 신청 - 성공")
    void testEnrollSuccess() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.countConfirmedByCourseId(courseId)).thenReturn(0);
        when(enrollmentRepository.findByCourseIdAndUserId(courseId, userId))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        // When
        EnrollmentResponse response = enrollmentService.enrollCourse(courseId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEnrollmentId()).isEqualTo(enrollmentId);
        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.PENDING.toString());
        assertThat(response.getUserId()).isEqualTo(userId);

        verify(courseRepository, times(1)).findById(courseId);
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("강의 미존재 - 실패")
    void testEnrollFailedCourseNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> enrollmentService.enrollCourse(courseId, userId))
                .isInstanceOf(CourseNotFoundException.class);

        verify(courseRepository, times(1)).findById(courseId);
    }

    @Test
    @DisplayName("강의 상태가 OPEN이 아님 - 실패")
    void testEnrollFailedCourseNotOpen() {
        // Given
        mockCourse.setStatus(CourseStatus.DRAFT);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(mockCourse));

        // When, Then
        assertThatThrownBy(() -> enrollmentService.enrollCourse(courseId, userId))
                .isInstanceOf(CourseNotOpenException.class);
    }

    @Test
    @DisplayName("정원 초과 - 실패")
    void testEnrollFailedCapacityExceeded() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.countConfirmedByCourseId(courseId))
                .thenReturn(mockCourse.getMaxCapacity());

        // When, Then
        assertThatThrownBy(() -> enrollmentService.enrollCourse(courseId, userId))
                .isInstanceOf(CapacityExceededException.class);
    }

    @Test
    @DisplayName("중복 신청 - 실패")
    void testEnrollFailedDuplicate() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.countConfirmedByCourseId(courseId)).thenReturn(0);
        when(enrollmentRepository.findByCourseIdAndUserId(courseId, userId))
                .thenReturn(Optional.of(mockEnrollment));

        // When, Then
        assertThatThrownBy(() -> enrollmentService.enrollCourse(courseId, userId))
                .isInstanceOf(DuplicateEnrollmentException.class);
    }

    @Test
    @DisplayName("결제 확정 - 성공")
    void testConfirmPaymentSuccess() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(enrollmentRepository.findByIdWithLock(enrollmentId))
                .thenReturn(Optional.of(mockEnrollment));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.countConfirmedByCourseId(courseId)).thenReturn(0);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        // When
        EnrollmentResponse response = enrollmentService.confirmPayment(enrollmentId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEnrollmentId()).isEqualTo(enrollmentId);

        verify(enrollmentRepository, times(1)).findByIdWithLock(enrollmentId);
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("결제 확정 - 신청 미존재")
    void testConfirmPaymentEnrollmentNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(enrollmentRepository.findByIdWithLock(enrollmentId))
                .thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> enrollmentService.confirmPayment(enrollmentId, userId))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }

    @Test
    @DisplayName("신청 취소 - 성공 (PENDING 상태)")
    void testCancelEnrollmentSuccess() {
        // Given
        mockEnrollment.setStatus(EnrollmentStatus.PENDING);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(enrollmentRepository.findByIdWithLock(enrollmentId))
                .thenReturn(Optional.of(mockEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        // When
        EnrollmentResponse response = enrollmentService.cancelEnrollment(enrollmentId, userId);

        // Then
        assertThat(response).isNotNull();
        verify(enrollmentRepository, times(1)).findByIdWithLock(enrollmentId);
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("신청 취소 - 이미 취소됨")
    void testCancelEnrollmentAlreadyCancelled() {
        // Given
        mockEnrollment.setStatus(EnrollmentStatus.CANCELLED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(enrollmentRepository.findByIdWithLock(enrollmentId))
                .thenReturn(Optional.of(mockEnrollment));

        // When, Then
        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(enrollmentId, userId))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    @DisplayName("권한 없음 - 다른 사용자의 신청 취소")
    void testCancelEnrollmentUnauthorized() {
        // Given
        String otherUserId = "student-999";
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(
                User.builder()
                        .id(otherUserId)
                        .name("다른학생")
                        .role(User.UserRole.STUDENT)
                        .createdAt(LocalDateTime.now())
                        .build()
        ));
        when(enrollmentRepository.findByIdWithLock(enrollmentId))
                .thenReturn(Optional.of(mockEnrollment));

        // When, Then
        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(enrollmentId, otherUserId))
                .isInstanceOf(UnauthorizedException.class);
    }
}