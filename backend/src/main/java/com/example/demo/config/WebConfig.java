package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // 이 클래스가 스프링 설정 클래스임을 나타냅니다.
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 1. CORS를 적용할 URL 패턴을 정의합니다. "/**"는 모든 URL을 의미합니다.
                .allowedOrigins("http://localhost:3000") // 2. 자원 공유를 허용할 Origin(출처)을 지정합니다.
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 3. 허용할 HTTP 메소드를 지정합니다.
                .allowedHeaders("*") // 4. 허용할 HTTP 요청 헤더를 지정합니다. "*"는 모든 헤더를 의미합니다.
                .allowCredentials(true) // 5. 쿠키를 포함한 요청을 허용할지 여부를 설정합니다.
                .maxAge(3600); // 6. Pre-flight 요청의 결과를 캐시할 시간을 초 단위로 설정합니다.
    }
}