package com.tftgogo.domain.admin.dto.response;

import com.tftgogo.domain.admin.entity.AdminRole;

public record AdminLoginResponse(
        String accessToken,
        String username,
        AdminRole role
) {}
