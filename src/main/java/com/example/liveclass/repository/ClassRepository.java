package com.example.liveclass.repository;

import com.example.liveclass.entity.Class;
import com.example.liveclass.entity.ClassStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<Class, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Class c WHERE c.id = ?1")
    Optional<Class> findByIdWithLock(String courseId);

    Page<Class> findByStatus(ClassStatus status, Pageable pageable);

    Page<Class> findByStatusIn(List<ClassStatus> statuses, Pageable pageable);

    Page<Class> findByCreatorId(String creatorId, Pageable pageable);

    Page<Class> findByCreatorIdAndStatus(String creatorId, ClassStatus status, Pageable pageable);

    Long countByStatus(ClassStatus status);

    List<Class> findAllByStatus(ClassStatus status);
}
