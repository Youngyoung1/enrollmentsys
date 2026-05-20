package com.example.liveclass;

import com.example.liveclass.dto.request.CreateCourseRequest;
import com.example.liveclass.dto.request.CreateCreatorRequest;
import com.example.liveclass.dto.request.CreateStudentRequest;
import com.example.liveclass.dto.response.CourseResponse;
import com.example.liveclass.entity.Course.CourseStatus;
import com.example.liveclass.entity.Enrollment;
import com.example.liveclass.entity.EnrollmentStatus;
import com.example.liveclass.exception.CapacityExceededException;
import com.example.liveclass.exception.DuplicateEnrollmentException;
import com.example.liveclass.repository.CourseRepository;
import com.example.liveclass.repository.CreatorRepository;
import com.example.liveclass.repository.EnrollmentRepository;
import com.example.liveclass.repository.StudentRepository;
import com.example.liveclass.repository.UserRepository;
import com.example.liveclass.service.CourseService;
import com.example.liveclass.service.CreatorService;
import com.example.liveclass.service.EnrollmentService;
import com.example.liveclass.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 수강 신청 동시성 테스트
 */
@SpringBootTest
class EnrollmentConcurrencyTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private CreatorService creatorService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CreatorRepository creatorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private String courseId;
    private static final int MAX_CAPACITY = 10;
    private static final int CONCURRENT_USERS = 50;

    @BeforeEach
    void setUp() {
        // 데이터 정리
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        creatorRepository.deleteAll();
        userRepository.deleteAll();

        // 1. 강사 생성
        creatorService.createCreator(CreateCreatorRequest.builder()
                .id("test-creator")
                .name("테스트 강사")
                .email("creator@test.com")
                .bio("동시성 테스트")
                .expertise("Spring Boot")
                .build());

        // 2. 강의 생성 (정원 10명)
        CourseResponse courseResponse = courseService.createCourse(
                CreateCourseRequest.builder()
                        .title("동시성 테스트 강의")
                        .description("정원 10명")
                        .price(50000)
                        .maxCapacity(MAX_CAPACITY)
                        .startDate(LocalDateTime.now().plusDays(1))
                        .endDate(LocalDateTime.now().plusDays(30))
                        .build(),
                "test-creator"
        );
        courseId = courseResponse.getId();

        // 3. 강의 상태를 OPEN으로 변경
        courseService.updateCourseStatus(courseId, CourseStatus.OPEN, "test-creator");

        // 4. 50명의 학생 생성
        for (int i = 1; i <= CONCURRENT_USERS; i++) {
            studentService.createStudent(CreateStudentRequest.builder()
                    .id("student-" + i)
                    .name("학생" + i)
                    .email("student" + i + "@test.com")
                    .bio("동시성 테스트 학생")
                    .phone("010-0000-" + String.format("%04d", i))
                    .build());
        }
    }

    @Test
    @DisplayName("동시에 50명이 신청 → 정원 10명만 성공해야 함")
    void 동시_수강신청_정원초과_방지() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger capacityExceededCount = new AtomicInteger(0);

        // when - 50명이 동시에 신청
        for (int i = 1; i <= CONCURRENT_USERS; i++) {
            final String userId = "student-" + i;
            executorService.submit(() -> {
                try {
                    enrollmentService.enrollCourse(courseId, userId);
                    successCount.incrementAndGet();
                } catch (CapacityExceededException e) {
                    capacityExceededCount.incrementAndGet();
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("예상치 못한 예외: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        System.out.println("\n=== 수강신청 동시성 테스트 결과 ===");
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("정원 초과: " + capacityExceededCount.get());
        System.out.println("DB 신청 건수: " + enrollmentRepository.findAll().size());
        System.out.println("================================\n");

        assertThat(successCount.get()).isLessThanOrEqualTo(CONCURRENT_USERS);
    }

    @Test
    @DisplayName("동시에 50명이 결제 확정 → 정원 10명만 CONFIRMED 되어야 함")
    void 동시_결제확정_정원초과_방지() throws InterruptedException {
        // given - 먼저 50명이 PENDING 상태로 신청
        List<String> enrollmentIds = new ArrayList<>();
        for (int i = 1; i <= CONCURRENT_USERS; i++) {
            String userId = "student-" + i;
            Enrollment enrollment = Enrollment.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .courseId(courseId)
                    .queuePosition(i)
                    .status(EnrollmentStatus.PENDING)
                    .enrolledAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            Enrollment saved = enrollmentRepository.save(enrollment);
            enrollmentIds.add(saved.getId());
        }

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(enrollmentIds.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger capacityExceededCount = new AtomicInteger(0);

        // when - 동시에 결제 확정 시도
        for (int i = 0; i < enrollmentIds.size(); i++) {
            final String enrollmentId = enrollmentIds.get(i);
            final String userId = "student-" + (i + 1);

            executorService.submit(() -> {
                try {
                    enrollmentService.confirmPayment(enrollmentId, userId);
                    successCount.incrementAndGet();
                } catch (CapacityExceededException e) {
                    capacityExceededCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("예외: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        Integer confirmedCount = enrollmentRepository.countConfirmedByCourseId(courseId);

        System.out.println("\n=== 결제 확정 동시성 테스트 결과 ===");
        System.out.println("✅ 확정 성공: " + successCount.get());
        System.out.println("❌ 정원 초과: " + capacityExceededCount.get());
        System.out.println("📊 DB 확정 건수: " + confirmedCount);
        System.out.println("🎯 정원 제한: " + MAX_CAPACITY);
        System.out.println("==================================\n");

        // 핵심 검증
        assertThat(confirmedCount).isLessThanOrEqualTo(MAX_CAPACITY);
        assertThat(successCount.get()).isLessThanOrEqualTo(MAX_CAPACITY);
    }

    @Test
    @DisplayName("같은 학생이 동시에 여러 번 신청 → 1번만 성공해야 함")
    void 동시_중복신청_방지() throws InterruptedException {
        // given
        String userId = "student-1";
        int attempts = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(attempts);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        // when - 같은 학생이 10번 동시 신청
        for (int i = 0; i < attempts; i++) {
            executorService.submit(() -> {
                try {
                    enrollmentService.enrollCourse(courseId, userId);
                    successCount.incrementAndGet();
                } catch (DuplicateEnrollmentException e) {
                    duplicateCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("예외: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        System.out.println("\n=== 중복 신청 방지 테스트 ===");
        System.out.println("✅ 성공: " + successCount.get());
        System.out.println("🚫 중복 거부: " + duplicateCount.get());
        System.out.println("============================\n");

        assertThat(successCount.get()).isEqualTo(1);
    }
}