package com.example.liveclass;

import com.example.liveclass.dto.response.CourseResponse;
import com.example.liveclass.dto.response.EnrollmentResponse;
import com.example.liveclass.dto.response.PaginatedResponse;
import com.example.liveclass.entity.Course;
import com.example.liveclass.entity.Course.CourseStatus;
import com.example.liveclass.entity.Creator;
import com.example.liveclass.entity.Enrollment;
import com.example.liveclass.entity.EnrollmentStatus;
import com.example.liveclass.entity.User;
import com.example.liveclass.exception.*;
import com.example.liveclass.repository.*;
import com.example.liveclass.service.CourseService;
import com.example.liveclass.service.EnrollmentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * 수강 신청 시스템 통합 테스트
 * 필수 + 선택 기능 모두 검증
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("수강 신청 시스템 통합 테스트")
class EnrollmentSysTest {

    @Autowired private EnrollmentService enrollmentService;
    @Autowired private CourseService courseService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CreatorRepository creatorRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;

    private static final int MAX_CAPACITY = 5;
    private String courseId;
    private String creatorId = "test-creator";

    @BeforeEach
    void setUp() {
        // 데이터 정리
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        creatorRepository.deleteAll();
        userRepository.deleteAll();

        // 강사 생성
        User creatorUser = User.builder()
                .id(creatorId)
                .name("테스트강사")
                .email("creator@test.com")
                .role(User.UserRole.CREATOR)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(creatorUser);

        Creator creator = Creator.builder()
                .id(creatorId)
                .bio("테스트")
                .expertise("Spring")
                .totalStudents(0)
                .avgRating(0.0)
                .enrolledAt(LocalDateTime.now())
                .build();
        creatorRepository.save(creator);

        // 강의 생성 (정원 5명)
        Course course = Course.builder()
                .id(UUID.randomUUID().toString())
                .creator(creator)
                .title("테스트 강의")
                .description("통합 테스트용 강의")
                .price(50000)
                .maxCapacity(MAX_CAPACITY)
                .currentEnrollment(0)
                .status(CourseStatus.OPEN)
                .startDate(LocalDateTime.now().plusDays(30))
                .endDate(LocalDateTime.now().plusDays(60))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        courseRepository.save(course);
        courseId = course.getId();

        // 학생 20명 생성
        for (int i = 1; i <= 20; i++) {
            User studentUser = User.builder()
                    .id("student-" + i)
                    .name("학생" + i)
                    .email("student" + i + "@test.com")
                    .role(User.UserRole.STUDENT)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(studentUser);
        }
    }

    // ========================================
    // 1. 강의 조회 테스트
    // ========================================

    @Test
    @Order(1)
    @DisplayName("강의 목록 조회 (상태 필터)")
    void 강의_목록_조회_상태_필터() {
        // OPEN 강의 조회
        PaginatedResponse<CourseResponse> openCourses =
                courseService.getCourses(CourseStatus.OPEN, PageRequest.of(0, 10));

        assertThat(openCourses.getContent()).hasSize(1);
        assertThat(openCourses.getContent().get(0).getStatus().toString()).isEqualTo("OPEN");

        // CLOSED 강의 조회 (없음)
        PaginatedResponse<CourseResponse> closedCourses =
                courseService.getCourses(CourseStatus.CLOSED, PageRequest.of(0, 10));

        assertThat(closedCourses.getContent()).isEmpty();

        // 전체 조회
        PaginatedResponse<CourseResponse> allCourses =
                courseService.getCourses(null, PageRequest.of(0, 10));

        assertThat(allCourses.getContent()).hasSize(1);
    }

    @Test
    @Order(2)
    @DisplayName("강의 상세 조회 (현재 신청 인원 포함)")
    void 강의_상세_조회() {
        // 학생 2명 신청
        enrollmentService.enrollCourse(courseId, "student-1");
        enrollmentService.enrollCourse(courseId, "student-2");

        // 결제 확정
        Enrollment e1 = enrollmentRepository.findByCourseIdAndUserId(courseId, "student-1").get();
        enrollmentService.confirmPayment(e1.getId(), "student-1");

        // 강의 상세 조회
        CourseResponse course = courseService.getCourse(courseId);

        assertThat(course.getId()).isEqualTo(courseId);
        assertThat(course.getTitle()).isEqualTo("테스트 강의");
        assertThat(course.getMaxCapacity()).isEqualTo(MAX_CAPACITY);
        assertThat(course.getCurrentEnrollment()).isEqualTo(1);  // CONFIRMED만 카운트
    }

    // ========================================
    // 2. 수강 신청 / 취소 테스트
    // ========================================

    @Test
    @Order(3)
    @DisplayName("수강 신청 - PENDING 상태 부여 + 순번 할당")
    void 수강_신청_PENDING_순번() {
        EnrollmentResponse response = enrollmentService.enrollCourse(courseId, "student-1");

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getQueuePosition()).isEqualTo(1);
        assertThat(response.getMessage()).contains("순번: 1");
    }

    @Test
    @Order(4)
    @DisplayName("수강 취소 - PENDING 상태")
    void 수강_취소_PENDING() {
        EnrollmentResponse enrolled = enrollmentService.enrollCourse(courseId, "student-1");

        EnrollmentResponse cancelled = enrollmentService.cancelEnrollment(
                enrolled.getEnrollmentId(), "student-1"
        );

        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.getCancelledAt()).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("내 수강 신청 목록 조회 (페이지네이션)")
    void 내_수강신청_목록() {
        // 3개 강의 만들기
        for (int i = 0; i < 3; i++) {
            Course course = Course.builder()
                    .id(UUID.randomUUID().toString())
                    .creator(creatorRepository.findById(creatorId).get())
                    .title("강의" + i)
                    .description("desc")
                    .price(10000)
                    .maxCapacity(10)
                    .currentEnrollment(0)
                    .status(CourseStatus.OPEN)
                    .startDate(LocalDateTime.now().plusDays(30))
                    .endDate(LocalDateTime.now().plusDays(60))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            courseRepository.save(course);
            enrollmentService.enrollCourse(course.getId(), "student-1");
        }

        // 페이지네이션 조회
        PaginatedResponse<EnrollmentResponse> page1 = enrollmentService.getMyEnrollments(
                "student-1", PageRequest.of(0, 2)
        );

        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.isFirst()).isTrue();
        assertThat(page1.isLast()).isFalse();

        PaginatedResponse<EnrollmentResponse> page2 = enrollmentService.getMyEnrollments(
                "student-1", PageRequest.of(1, 2)
        );

        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.isLast()).isTrue();
    }

    // ========================================
    // 3. 정원 관리 규칙 테스트
    // ========================================

    @Test
    @Order(6)
    @DisplayName("정원 초과 - WAITING 상태로 처리")
    void 정원_초과시_WAITING() {
        // 정원 5명 채우기 (PENDING)
        for (int i = 1; i <= MAX_CAPACITY; i++) {
            EnrollmentResponse res = enrollmentService.enrollCourse(courseId, "student-" + i);
            assertThat(res.getStatus()).isEqualTo("PENDING");
            assertThat(res.getQueuePosition()).isEqualTo(i);
        }

        // 6번째 학생 → WAITING
        EnrollmentResponse waiting = enrollmentService.enrollCourse(courseId, "student-6");
        assertThat(waiting.getStatus()).isEqualTo("WAITING");
        assertThat(waiting.getQueuePosition()).isEqualTo(6);
        assertThat(waiting.getMessage()).contains("대기열");
    }

    @Test
    @Order(7)
    @DisplayName("동시 신청 - 정원 5명, 20명 동시 신청 → 정확히 5명만 PENDING")
    void 동시_신청_정원_제어() throws InterruptedException {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        AtomicInteger pendingCount = new AtomicInteger(0);
        AtomicInteger waitingCount = new AtomicInteger(0);

        for (int i = 1; i <= threads; i++) {
            final String userId = "student-" + i;
            executor.submit(() -> {
                try {
                    EnrollmentResponse res = enrollmentService.enrollCourse(courseId, userId);
                    if ("PENDING".equals(res.getStatus())) pendingCount.incrementAndGet();
                    else if ("WAITING".equals(res.getStatus())) waitingCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("예외: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("\n=== 동시 신청 결과 ===");
        System.out.println("PENDING: " + pendingCount.get());
        System.out.println("WAITING: " + waitingCount.get());

        assertThat(pendingCount.get()).isEqualTo(MAX_CAPACITY);
        assertThat(waitingCount.get()).isEqualTo(threads - MAX_CAPACITY);
    }

    // ========================================
    // 4. 취소 기간 제한 (선택)
    // ========================================

    @Test
    @Order(8)
    @DisplayName("CONFIRMED 후 7일 이내 취소 - 성공")
    void 취소_7일_이내_성공() {
        EnrollmentResponse enrolled = enrollmentService.enrollCourse(courseId, "student-1");
        EnrollmentResponse confirmed = enrollmentService.confirmPayment(
                enrolled.getEnrollmentId(), "student-1"
        );

        // 5일 전에 확정한 것으로 조작
        Enrollment e = enrollmentRepository.findById(confirmed.getEnrollmentId()).get();
        e.setConfirmedAt(LocalDateTime.now().minusDays(5));
        enrollmentRepository.save(e);

        // 취소 성공
        EnrollmentResponse cancelled = enrollmentService.cancelEnrollment(
                enrolled.getEnrollmentId(), "student-1"
        );
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @Order(9)
    @DisplayName("CONFIRMED 후 7일 초과 취소 - 실패")
    void 취소_7일_초과_실패() {
        EnrollmentResponse enrolled = enrollmentService.enrollCourse(courseId, "student-1");
        EnrollmentResponse confirmed = enrollmentService.confirmPayment(
                enrolled.getEnrollmentId(), "student-1"
        );

        // 8일 전에 확정한 것으로 조작
        Enrollment e = enrollmentRepository.findById(confirmed.getEnrollmentId()).get();
        e.setConfirmedAt(LocalDateTime.now().minusDays(8));
        enrollmentRepository.save(e);

        // 취소 실패
        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(
                enrolled.getEnrollmentId(), "student-1"
        )).isInstanceOf(CancellationPeriodExceededException.class);
    }

    // ========================================
    // 5. 대기열(Waitlist) 기능 (선택)
    // ========================================

    @Test
    @Order(10)
    @DisplayName("대기열 자동 승격 - PENDING 취소 시 WAITING → PENDING")
    void 대기열_자동_승격() {
        // 5명 PENDING + 2명 WAITING
        for (int i = 1; i <= MAX_CAPACITY; i++) {
            enrollmentService.enrollCourse(courseId, "student-" + i);
        }
        enrollmentService.enrollCourse(courseId, "student-6");  // WAITING (position=6)
        enrollmentService.enrollCourse(courseId, "student-7");  // WAITING (position=7)

        // student-1 (PENDING) 취소
        Enrollment e1 = enrollmentRepository.findByCourseIdAndUserId(courseId, "student-1").get();
        enrollmentService.cancelEnrollment(e1.getId(), "student-1");

        // student-6이 자동으로 PENDING으로 승격
        Enrollment promoted = enrollmentRepository
                .findByCourseIdAndUserId(courseId, "student-6").get();
        assertThat(promoted.getStatus()).isEqualTo(EnrollmentStatus.PENDING);

        // student-7은 여전히 WAITING
        Enrollment stillWaiting = enrollmentRepository
                .findByCourseIdAndUserId(courseId, "student-7").get();
        assertThat(stillWaiting.getStatus()).isEqualTo(EnrollmentStatus.WAITING);
    }

    // ========================================
    // 6. 강의별 수강생 목록 (선택)
    // ========================================

    @Test
    @Order(11)
    @DisplayName("강의별 수강생 목록 - 페이지네이션")
    void 강의별_수강생_목록() {
        // 7명 신청 (5 PENDING + 2 WAITING)
        for (int i = 1; i <= 7; i++) {
            enrollmentService.enrollCourse(courseId, "student-" + i);
        }

        // 페이지 1 (5명)
        PaginatedResponse<EnrollmentResponse> page1 = enrollmentService.getEnrollmentsByCourse(
                courseId, PageRequest.of(0, 5)
        );

        assertThat(page1.getContent()).hasSize(5);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.isFirst()).isTrue();

        // 페이지 2 (2명)
        PaginatedResponse<EnrollmentResponse> page2 = enrollmentService.getEnrollmentsByCourse(
                courseId, PageRequest.of(1, 5)
        );

        assertThat(page2.getContent()).hasSize(2);
        assertThat(page2.isLast()).isTrue();
    }

    // ========================================
    // 7. 중복 신청 방지 테스트
    // ========================================

    @Test
    @Order(12)
    @DisplayName("동일 사용자 중복 신청 → 거부")
    void 중복_신청_거부() {
        enrollmentService.enrollCourse(courseId, "student-1");

        assertThatThrownBy(() -> enrollmentService.enrollCourse(courseId, "student-1"))
                .isInstanceOf(DuplicateEnrollmentException.class);
    }

    // ========================================
    // 8. 결제 확정 테스트
    // ========================================

    @Test
    @Order(13)
    @DisplayName("결제 확정 - PENDING → CONFIRMED")
    void 결제_확정_성공() {
        EnrollmentResponse enrolled = enrollmentService.enrollCourse(courseId, "student-1");

        EnrollmentResponse confirmed = enrollmentService.confirmPayment(
                enrolled.getEnrollmentId(), "student-1"
        );

        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getConfirmedAt()).isNotNull();

        // Course의 currentEnrollment 증가 확인
        Course course = courseRepository.findById(courseId).get();
        assertThat(course.getCurrentEnrollment()).isEqualTo(1);
    }
}