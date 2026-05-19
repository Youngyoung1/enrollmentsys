package com.example.liveclass.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server; // ✨ 추가됨
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // ✨ [추가] CORS 에러 방지를 위한 서버 URL 정의
        Server prodServer = new Server();
        prodServer.setUrl("https://enrollmentsys-production.up.railway.app");
        prodServer.setDescription("Production Server (Railway)");

        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Server (내 컴퓨터)");

        return new OpenAPI()
                // ✨ [추가] 서버 리스트를 가장 먼저 주입합니다.
                .servers(List.of(prodServer, localServer))
                .info(new Info()
                        .title("LiveClass API")
                        .version("1.0.0")
                        .description("""
                                ## 사용 순서
                                1. **Users** - 강사/학생 회원가입 → ID 획득
                                2. **강의관리** - 강사가 강의 생성/관리
                                3. **수강신청** - 학생이 강의 신청/확정/취소
                                
                                ## 인증 방법
                                우측 상단 🔒 **Authorize** 버튼 → Value에 ID 입력
                                - 강사: `creator-1`
                                - 학생: `student-1`
                                """))
                .tags(List.of(
                        new Tag().name("Users").description("👤 사용자 관리"),
                        new Tag().name("강의관리").description("🎬 강의 관리"),
                        new Tag().name("수강신청").description("📝 수강 신청")
                ))
                .components(new Components()
                        .addSecuritySchemes("ApiKeyAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("ID")
                                        .name("Authorization")
                                        .description("사용자 ID 입력 (예: creator-1, student-1)")))
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"));
    }
}