package com.tftgogo.global.config;

import com.tftgogo.domain.admin.web.AdminAuditInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // @WebMvcTest 슬라이스에서는 도메인 @Component가 스캔되지 않으므로 optional로 주입
    @Nullable
    @Autowired(required = false)
    private AdminAuditInterceptor adminAuditInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (adminAuditInterceptor == null) return;
        registry.addInterceptor(adminAuditInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                        "/api/admin/auth/login",
                        "/api/admin/auth/refresh",
                        "/api/admin/auth/logout"
                );
    }
}
