package com.example.liveclass.service;

import com.example.liveclass.dto.request.CreateStudentRequest;
import com.example.liveclass.dto.response.StudentRegistrationResponse;
import com.example.liveclass.entity.Student;
import com.example.liveclass.entity.User;
import com.example.liveclass.exception.DuplicateException;
import com.example.liveclass.repository.StudentRepository;
import com.example.liveclass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 학생(Student) 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    /**
     * 학생 회원가입 (기존 메서드 - String 반환)
     */
    public String createStudent(CreateStudentRequest request) {
        log.info("학생 회원가입 요청: {}", request.getId());

        if (userRepository.existsById(request.getId())) {
            throw new DuplicateException("이미 존재하는 사용자 ID입니다: " + request.getId());
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateException("이미 존재하는 이메일입니다: " + request.getEmail());
        }

        User user = User.builder()
                .id(request.getId())
                .name(request.getName())
                .email(request.getEmail())
                .role(User.UserRole.STUDENT)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("User 생성됨: {}", request.getId());

        Student student = Student.builder()
                .id(request.getId())
                .bio(request.getBio())
                .phone(request.getPhone())
                .enrolledAt(LocalDateTime.now())
                .build();

        studentRepository.save(student);
        log.info("Student 생성됨: {}", request.getId());

        return request.getId();
    }

    /**
     * 학생 회원가입 (응답 DTO 포함 - UUID 반환)
     */
    public StudentRegistrationResponse createStudentWithResponse(CreateStudentRequest request) {
        String studentId = createStudent(request);  // 기존 로직 재사용

        LocalDateTime now = LocalDateTime.now();
        return StudentRegistrationResponse.builder()
                .studentId(studentId)  // ✅ UUID 반환
                .name(request.getName())
                .email(request.getEmail())
                .bio(request.getBio())
                .phone(request.getPhone())
                .enrolledAt(now)
                .message("학생 회원가입 성공")
                .build();
    }
}