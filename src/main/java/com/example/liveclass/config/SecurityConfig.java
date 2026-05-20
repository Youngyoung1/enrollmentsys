package com.example.liveclass.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 및 H2 콘솔 프레임 제한 해제
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                // 2. HTTP 요청에 대한 보안 라우팅 설정
                .authorizeHttpRequests(auth -> auth
                        // 💡 Swagger UI, OpenAPI 메타데이터, 파비콘 및 H2 콘솔을 안전하게 필터단에서 전체 허용합니다.
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/h2-console/**",
                                "/favicon.ico"
                        ).permitAll()

                        // 3. 그 외 나머지 서비스 API 요청들도 우선 모두 허용 (이후 개발 완료 시 .authenticated()로 변경)
                        .anyRequest().permitAll()
                );

        return http.build();
    }

}