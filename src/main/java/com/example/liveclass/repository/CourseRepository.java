package com.example.liveclass.repository;

import com.example.liveclass.entity.Course;
import com.example.liveclass.entity.Course.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 강의 Repository
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, String> {

    /**
     * ID로 조회 (비관적 락 적용)
     * 동시성 제어를 위해 SELECT FOR UPDATE 사용
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findByIdWithLock(@Param("id") String id);

    /**
     * 상태로 필터링 조회 (페이지네이션)
     */
    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    /**
     * 강사별 강의 조회 (페이지네이션)
     */
    Page<Course> findByCreatorId(String creatorId, Pageable pageable);

    List<Course> findByStatusAndStartDateBefore(CourseStatus courseStatus, LocalDateTime now);
}