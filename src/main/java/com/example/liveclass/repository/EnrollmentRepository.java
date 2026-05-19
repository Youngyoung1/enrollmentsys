package com.example.liveclass.repository;

import com.example.liveclass.entity.Enrollment;
import com.example.liveclass.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 신청 저장소
 * 신청 데이터베이스 접근
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {

    /**
     * 강의 + 사용자로 신청 조회
     */
    Optional<Enrollment> findByCourseIdAndUserId(String courseId, String userId);

    /**
     * 강의 + 사용자 + 상태로 신청 조회
     */
    Optional<Enrollment> findByCourseIdAndUserIdAndStatus(
            String courseId,
            String userId,
            EnrollmentStatus status
    );

    /**
     * 강의별 확정된 신청 개수 (정원 계산용)
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.courseId = ?1 AND e.status = 'CONFIRMED'")
    Integer countConfirmedByCourseId(String courseId);

    /**
     * 사용자의 신청 목록 (상태 필터)
     */
    Page<Enrollment> findByUserIdAndStatus(String userId, EnrollmentStatus status, Pageable pageable);

    /**
     * 사용자의 신청 목록 (상태 복수 필터)
     */
    Page<Enrollment> findByUserIdAndStatusIn(String userId, List<EnrollmentStatus> statuses, Pageable pageable);

    /**
     * 사용자의 모든 신청 목록
     */
    Page<Enrollment> findByUserId(String userId, Pageable pageable);

    /**
     * 강의별 모든 신청 개수
     */
    Long countByCourseId(String courseId);

    /**
     * 강의별 확정된 신청 목록
     */
    List<Enrollment> findByCourseIdAndStatus(String courseId, EnrollmentStatus status);

    Page<Enrollment> findByCourseId(String courseId, Pageable pageable);
}