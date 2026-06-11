package com.tftgogo.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequest {

    @NotBlank(message = "보낸 사람 이름은 필수입니다.")
    @Size(max = 30, message = "보낸 사람 이름은 30자 이하여야 합니다.")
    private String senderName;

    @Size(max = 30, message = "티어는 30자 이하여야 합니다.")
    private String senderTier;

    @NotBlank(message = "메시지는 필수입니다.")
    @Size(max = 500, message = "메시지는 500자 이하여야 합니다.")
    private String message;
}
