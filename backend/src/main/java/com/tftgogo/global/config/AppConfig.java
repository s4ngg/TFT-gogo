package com.tftgogo.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import com.tftgogo.global.riot.config.RiotProperties;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(RiotProperties.class)
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Spring Boot 3.2+에서 RestTemplateBuilder.connectTimeout(Duration) 메서드가 제거됨
        // 따라서 SimpleClientHttpRequestFactory를 직접 생성해 타임아웃을 설정해야 함
        // 참고: https://github.com/spring-projects/spring-boot/issues/38549
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);  // 10초
        factory.setReadTimeout(10_000);     // 10초
        return new RestTemplate(factory);
    }
}
