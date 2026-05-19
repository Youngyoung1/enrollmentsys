package com.example.liveclass.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 설정
 * 브라우저에서 http://localhost:8080/swagger-ui.html 접속
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🎓 수강 신청 시스템 API")
                        .version("1.0.0")
                        .description("""
                                온라인 강의 플랫폼의 수강 신청 시스템 API 문서입니다.
                                
                                주요 기능:
                                - 강의 관리 (생성, 상태 변경, 조회)
                                - 수강 신청 (신청, 확정, 취소)
                                - 정원 관리 (초과 방지, 동시성 제어)
                                - 취소 기간 제한 (확정 후 7일)
                                """)
                        .contact(new Contact()
                                .name("라이브클래스")
                                .url("https://liveclass.com")
                                .email("support@liveclass.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("로컬 개발 서버"))
                .addServersItem(new Server()
                        .url("https://liveclass-production.up.railway.app")
                        .description("프로덕션 서버"));
    }
}