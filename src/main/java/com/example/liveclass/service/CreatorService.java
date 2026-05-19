package com.example.liveclass.service;

import com.example.liveclass.dto.request.CreateCreatorRequest;
import com.example.liveclass.dto.response.CreatorRegistrationResponse;
import com.example.liveclass.entity.Creator;
import com.example.liveclass.entity.User;
import com.example.liveclass.exception.DuplicateException;
import com.example.liveclass.repository.CreatorRepository;
import com.example.liveclass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 강사(Creator) 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CreatorService {

    private final CreatorRepository creatorRepository;
    private final UserRepository userRepository;

    /**
     * 강사 회원가입 (기존 메서드 - String 반환)
     */
    public String createCreator(CreateCreatorRequest request) {
        log.info("강사 회원가입 요청: {}", request.getId());

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
                .role(User.UserRole.CREATOR)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("User 생성됨: {}", request.getId());

        Creator creator = Creator.builder()
                .id(request.getId())
                .bio(request.getBio())
                .expertise(request.getExpertise())
                .totalStudents(0)
                .avgRating(0.0)
                .enrolledAt(LocalDateTime.now())
                .build();

        creatorRepository.save(creator);
        log.info("Creator 생성됨: {}", request.getId());

        return request.getId();
    }

    /**
     * 강사 회원가입 (응답 DTO 포함 - UUID 반환)
     */
    public CreatorRegistrationResponse createCreatorWithResponse(CreateCreatorRequest request) {
        String creatorId = createCreator(request);  // 기존 로직 재사용

        LocalDateTime now = LocalDateTime.now();
        return CreatorRegistrationResponse.builder()
                .creatorId(creatorId)  // ✅ UUID 반환
                .name(request.getName())
                .email(request.getEmail())
                .bio(request.getBio())
                .expertise(request.getExpertise())
                .enrolledAt(now)
                .message("강사 회원가입 성공")
                .build();
    }
}