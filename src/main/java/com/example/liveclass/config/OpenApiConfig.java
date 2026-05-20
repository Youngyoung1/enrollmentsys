package com.example.liveclass.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 로컬 환경을 위한 기본 서버 주소
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("로컬 개발 서버");

        // Railway 클라우드 배포 환경을 위한 상대 경로 서버 주소 (도메인이 바뀌어도 자동으로 따라갑니다)
        Server productionServer = new Server();
        productionServer.setUrl("/");
        productionServer.setDescription("Railway 배포 운영 서버");

        return new OpenAPI()
                .info(new Info()
                        .title("LiveClass 수강신청 시스템 API 명세서")
                        .version("v1.0.0")
                        .description("로컬 및 Railway 클라우드 배포 통합 명세서"))
                .servers(List.of(localServer, productionServer));
    }
}