package com.tftgogo.domain.community.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageCreateRequest {

    @NotBlank(message = "채팅방 ID는 필수입니다.")
    private String roomId;

    @NotBlank(message = "보낸 사람 이름은 필수입니다.")
    @Size(max = 40, message = "보낸 사람 이름은 40자 이하로 입력해주세요.")
    private String senderName;

    @Size(max = 40, message = "티어는 40자 이하로 입력해주세요.")
    private String tier;

    @NotBlank(message = "메시지는 필수입니다.")
    @Size(max = 500, message = "메시지는 500자 이하로 입력해주세요.")
    private String content;
}
