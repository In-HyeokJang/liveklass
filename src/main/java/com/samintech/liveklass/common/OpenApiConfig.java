package com.samintech.liveklass.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LiveKlass API")
                        .description("수강 신청 시스템 REST API\n\n" +
                                "## 인증\n" +
                                "쓰기 API는 `X-User-Id` 헤더로 사용자 ID를 전달합니다. (JWT 미사용)\n\n" +
                                "## 사용자 역할\n" +
                                "- **CREATOR**: 강의 생성·상태 변경·수강생 목록 조회\n" +
                                "- **CLASSMATE**: 수강 신청·결제 확정·취소·내 신청 목록 조회\n\n" +
                                "## 강의 상태 머신\n" +
                                "`DRAFT → OPEN → CLOSED` (역방향 불가)\n\n" +
                                "## 수강 신청 상태 머신\n" +
                                "`PENDING → CONFIRMED → CANCELLED`\n" +
                                "정원 초과 시: `WAITLISTED → PENDING` (앞 수강생 취소 시 자동 승격)\n\n" +
                                "## 강의 목록 조회 필터 (`GET /api/courses`)\n" +
                                "`status`, `title`, `minPrice`, `maxPrice`, `startDate`, `endDate`, `hasVacancies` 조합 가능\n\n" +
                                "## 시드 데이터 (서버 기동 시 자동 생성 — 재시작 시 초기화됨)\n" +
                                "**사용자**\n\n" +
                                "| ID | username | role |\n" +
                                "|---|---|---|\n" +
                                "| 1~5 | creator1~5 | CREATOR |\n" +
                                "| 6~20 | student1~15 | CLASSMATE |\n\n" +
                                "**강의** (ID 1~5): Java 마스터(OPEN/정원10), JPA 심화(OPEN/정원2·만석+대기열), HTML기초(DRAFT), React(CLOSED), 알고리즘(OPEN/정원5)\n\n" +
                                "> 사용자 직접 추가: `POST /api/users`")
                        .version("1.0.0"));
    }
}