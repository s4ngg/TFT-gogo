package com.tftgogo.domain.admin.security;

import com.tftgogo.domain.admin.entity.AdminRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AdminPrincipal {
    private final Long adminId;
    private final String username;
    private final AdminRole role;
}
