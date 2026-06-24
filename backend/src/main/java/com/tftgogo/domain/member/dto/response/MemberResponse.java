package com.tftgogo.domain.member.dto.response;

import com.tftgogo.domain.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberResponse {

    private Long id;
    private String email;
    private String nickname;
    private String profileImage;
    private boolean notificationEnabled;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getUserId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImage(member.getProfileImage())
                .notificationEnabled(member.isNotificationEnabled())
                .build();
    }


}
