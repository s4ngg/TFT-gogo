package com.tftgogo.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /** CORS 허용 origin 목록 (allowCredentials=true이므로 wildcard 사용 불가) */
    private List<String> allowedOrigins = List.of("http://localhost:5173", "http://127.0.0.1:5173");
}
