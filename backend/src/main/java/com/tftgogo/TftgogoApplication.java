package com.tftgogo;

import com.tftgogo.domain.admin.config.AdminBootstrapProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class TftgogoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TftgogoApplication.class, args);
    }
}
