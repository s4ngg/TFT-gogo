package com.tftgogo.global.config;

import com.tftgogo.global.riot.config.RiotProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(RiotProperties riotProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(riotProperties.getConnectTimeoutMs());
        factory.setReadTimeout(riotProperties.getReadTimeoutMs());
        return new RestTemplate(factory);
    }
}
