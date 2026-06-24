package com.tftgogo.domain.member.service;

import com.tftgogo.domain.member.dto.response.SocialLoginStartResponse;

public interface SocialLoginStartService {

    SocialLoginStartResponse getStartUrl(String provider, String baseUrl);
}
