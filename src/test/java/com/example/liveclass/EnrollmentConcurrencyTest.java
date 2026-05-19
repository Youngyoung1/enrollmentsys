package com.example.liveclass;

import com.example.liveclass.dto.request.EnrollRequest;
import com.example.liveclass.entity.CourseStatus;
import com.example.liveclass.exception.CapacityExceededException;
import com.example.liveclass.repository.CourserRepository;
import com.example.liveclass.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
// import org.springframework.transaction.annotation.Transactional; // 이 줄을 제거합니다.

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * 동시성 테스트
 * 비관적 락을 사용하여 정원 초과를 방지하는지 검증
 */
@SpringBootTest(classes = EnrollmentSystemApplication.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
// @Transactional // 이 어노테이션을 제거합니다.
@DisplayName("동시성 제어 테스트")
public class EnrollmentConcurrencyTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private CourserRepository courserRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private String courseId;
    private String creatorId;

    @BeforeEach
    void setUp() {
        // 테스트 시작 전 DB를 클린업 (선택 사항이지만, @Transactional 제거 시 필요할 수 있음)
        enrollmentRepository.deleteAll();
        courserRepository.deleteAll();

        // 정원 1명인 강의 생성
        Class course = Class.builder()
                .id(UUID.randomUUID().toString())
                .creatorId("creator-1")
                .title("동시성 테스트 강의")
                .description("정원 1명인 강의")
                .price(50000)
                .maxCapacity(1)  // 정원 1명
                .currentEnrollment(0)
                .status(CourseStatus.OPEN)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .build();

        Class saved = courserRepository.save(course);
        courseId = saved.getId();
        creatorId = saved.getCreatorId();
    }

    @Test
    @DisplayName("정원 1명, 100명 동시 신청 시 정원 초과 방지")
    void testConcurrentEnrollmentWithCapacityOne() throws InterruptedException {
        int numThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int studentNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    EnrollRequest request = new EnrollRequest();
                    request.setCourseId(courseId);

                    String userId = "student-" + studentNum;
                    enrollmentService.enroll(request, userId);
                    successCount.incrementAndGet();

                } catch (CapacityExceededException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(99);

        Long totalEnrollments = enrollmentRepository.countByCourseId(courseId);
        assertThat(totalEnrollments).isEqualTo(1);

        Class course = courserRepository.findById(courseId).orElseThrow();
        // ⭐ 수정됨: enroll 시점에 currentEnrollment가 증가하므로 1이 되어야 합니다.
        assertThat(course.getCurrentEnrollment()).isEqualTo(1);
    }

    @Test
    @DisplayName("정원 10명, 100명 동시 신청 시 10명만 성공")
    void testConcurrentEnrollmentWithCapacityTen() throws InterruptedException {
        // 정원을 10명으로 변경
        Class course = courserRepository.findById(courseId).orElseThrow();
        course.setMaxCapacity(10);
        courserRepository.save(course);

        int numThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int studentNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    EnrollRequest request = new EnrollRequest();
                    request.setCourseId(courseId);

                    String userId = "student-" + studentNum;
                    enrollmentService.enroll(request, userId);
                    successCount.incrementAndGet();

                } catch (CapacityExceededException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failureCount.get()).isEqualTo(90);

        Long totalEnrollments = enrollmentRepository.countByCourseId(courseId);
        assertThat(totalEnrollments).isEqualTo(10);

        Class courseAfter = courserRepository.findById(courseId).orElseThrow();
        // ⭐ 추가됨: enroll 시점에 currentEnrollment가 증가하므로 10이 되어야 합니다.
        assertThat(courseAfter.getCurrentEnrollment()).isEqualTo(10);
    }

    @Test
    @DisplayName("중복 신청 방지 테스트")
    void testDuplicateEnrollmentPrevention() throws InterruptedException {
        int numAttempts = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numAttempts);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numAttempts);

        String userId = "student-duplicate";
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        for (int i = 0; i < numAttempts; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    EnrollRequest request = new EnrollRequest();
                    request.setCourseId(courseId);

                    enrollmentService.enroll(request, userId);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    duplicateCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(duplicateCount.get()).isEqualTo(9);

        var enrollments = enrollmentRepository.findByCourseIdAndUserId(courseId, userId);
        assertThat(enrollments).isPresent();
    }
}
