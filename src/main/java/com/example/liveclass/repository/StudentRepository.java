package com.example.liveclass.repository;

import com.example.liveclass.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 학생 Repository
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, String> {

}