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

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Enrollment e WHERE e.id = :id")
    Optional<Enrollment> findByIdWithLock(@Param("id") String id);

    Optional<Enrollment> findByCourseIdAndUserId(String courseId, String userId);

    Optional<Enrollment> findByCourseIdAndUserIdAndStatus(
            String courseId, String userId, EnrollmentStatus status
    );

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.courseId = :courseId " +
            "AND e.status = com.example.liveclass.entity.EnrollmentStatus.CONFIRMED")
    Integer countConfirmedByCourseId(@Param("courseId") String courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.courseId = :courseId " +
            "AND e.status IN (com.example.liveclass.entity.EnrollmentStatus.PENDING, " +
            "com.example.liveclass.entity.EnrollmentStatus.CONFIRMED)")
    Integer countActiveByCourseId(@Param("courseId") String courseId);

    Long countByCourseId(String courseId);

    /**
     * 사용자 + 상태별 신청 목록 (@Query 사용)
     */
    @Query("SELECT e FROM Enrollment e WHERE e.userId = :userId AND e.status = :status")
    Page<Enrollment> findByUserIdAndStatus(
            @Param("userId") String userId,
            @Param("status") EnrollmentStatus status,
            Pageable pageable
    );

    /**
     * 사용자 + 상태 목록 신청 (@Query 사용)
     */
    @Query("SELECT e FROM Enrollment e WHERE e.userId = :userId AND e.status IN :statuses")
    Page<Enrollment> findByUserIdAndStatusIn(
            @Param("userId") String userId,
            @Param("statuses") List<EnrollmentStatus> statuses,
            Pageable pageable
    );

    Page<Enrollment> findByUserId(String userId, Pageable pageable);

    Page<Enrollment> findByCourseId(String courseId, Pageable pageable);

    List<Enrollment> findByCourseIdAndStatus(String courseId, EnrollmentStatus status);

    @Query("SELECT COALESCE(MAX(e.queuePosition), 0) FROM Enrollment e " +
            "WHERE e.courseId = :courseId " +
            "AND e.status != com.example.liveclass.entity.EnrollmentStatus.CANCELLED")
    Integer findMaxQueuePosition(@Param("courseId") String courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Enrollment e WHERE e.courseId = :courseId " +
            "AND e.status = com.example.liveclass.entity.EnrollmentStatus.WAITING " +
            "ORDER BY e.queuePosition ASC")
    List<Enrollment> findFirstWaitingByCourseIdWithLock(@Param("courseId") String courseId);
}