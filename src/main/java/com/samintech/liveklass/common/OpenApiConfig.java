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
                                "**인증**: 모든 쓰기 API는 `X-User-Id` 헤더로 사용자 ID를 전달합니다.\n\n" +
                                "**시드 데이터**: creator(id=1,2), classmate(id=3,4,5)")
                        .version("1.0.0"));
    }
}