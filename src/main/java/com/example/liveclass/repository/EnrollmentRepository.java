package com.example.liveclass.repository;

import com.example.liveclass.entity.Enrollment;
import com.example.liveclass.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * 수강 신청 Repository
 * 신청 데이터베이스 접근
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {

    /**
     * ID로 조회 (비관적 락 적용)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Enrollment e WHERE e.id = :id")
    Optional<Enrollment> findByIdWithLock(@Param("id") String id);

    /**
     * 강의 + 사용자로 신청 조회
     */
    Optional<Enrollment> findByCourseIdAndUserId(@Param("courseId") String courseId, @Param("userId") String userId);

    /**
     * 강의 + 사용자 + 상태로 신청 조회
     */
    Optional<Enrollment> findByCourseIdAndUserIdAndStatus(
            @Param("courseId") String courseId,
            @Param("userId") String userId,
            @Param("status") EnrollmentStatus status
    );

    /**
     * 강의별 확정된 신청 개수 (정원 계산용)
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.courseId = :courseId AND e.status = 'CONFIRMED'")
    Integer countConfirmedByCourseId(@Param("courseId") String courseId);

    /**
     * 사용자의 신청 목록 (상태 필터)
     */
    Page<Enrollment> findByUserIdAndStatus(
            @Param("userId") String userId,
            @Param("status") EnrollmentStatus status,
            Pageable pageable
    );

    /**
     * 사용자의 신청 목록 (상태 복수 필터)
     */
    Page<Enrollment> findByUserIdAndStatusIn(
            @Param("userId") String userId,
            @Param("statuses") List<EnrollmentStatus> statuses,
            Pageable pageable
    );

    /**
     * 사용자의 모든 신청 목록
     */
    Page<Enrollment> findByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * 강의별 모든 신청 개수
     */
    Long countByCourseId(@Param("courseId") String courseId);

    /**
     * 강의별 확정된 신청 목록
     */
    List<Enrollment> findByCourseIdAndStatus(
            @Param("courseId") String courseId,
            @Param("status") EnrollmentStatus status
    );

    /**
     * 강의별 신청 목록 (페이지네이션)
     */
    Page<Enrollment> findByCourseId(@Param("courseId") String courseId, Pageable pageable);
}