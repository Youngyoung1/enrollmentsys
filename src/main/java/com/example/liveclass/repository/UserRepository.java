package com.example.liveclass.repository;

import com.example.liveclass.entity.User;
import jakarta.validation.constraints.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 사용자 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByEmail(@Email(message = "유효한 이메일 주소여야 합니다") String email);
}