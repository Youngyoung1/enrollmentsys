package com.example.liveclass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 수강 신청 시스템 Spring Boot 애플리케이션
 * 메인 진입점
 */
@SpringBootApplication
public class EnrollmentSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnrollmentSystemApplication.class, args);
    }
}
