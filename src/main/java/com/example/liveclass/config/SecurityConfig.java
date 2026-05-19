package com.example.liveclass.config; // ⚠️ 본인의 실제 패키지 경로로 맞추기

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 💡 무기 1: Security 필터 자체를 아예 통과하지 않도록 예외(Ignore) 처리하기
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/h2-console/**",
                "/favicon.ico"
        );
    }

    // 💡 무기 2: Http 필터에서도 모든 접근 권한 허용 및 CSRF 해제
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API 테스트를 위해 CSRF 비활성화
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // H2 콘솔용
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // ⚠️ 테스트를 위해 우선 모든 API 경로를 전면 개방합니다.
                );

        return http.build();
    }
}