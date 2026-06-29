package com.tftgogo.global.config;

import com.tftgogo.domain.admin.service.AdminAuditService;
import com.tftgogo.domain.admin.web.AdminAuditInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // AdminAuditService는 도메인 @Service라서 @WebMvcTest 슬라이스에 포함되지 않는다.
    // optional로 주입하고 null이면 interceptor를 등록하지 않아 슬라이스 테스트와 호환된다.
    // AdminAuditInterceptor에는 @Component가 없으므로 @WebMvcTest에 자동 스캔되지 않는다.
    @Nullable
    @Autowired(required = false)
    private AdminAuditService adminAuditService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (adminAuditService == null) return;
        registry.addInterceptor(new AdminAuditInterceptor(adminAuditService))
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                        "/api/admin/auth/login",
                        "/api/admin/auth/refresh",
                        "/api/admin/auth/logout"
                );
    }
}
