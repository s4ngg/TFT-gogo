package com.tftgogo.domain.deck.dto.response;

import com.tftgogo.domain.deck.entity.ClientVersionPatchMapping;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ClientVersionPatchMappingResponse {

    private Long id;
    private String clientVersion;
    private String patchVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ClientVersionPatchMappingResponse from(ClientVersionPatchMapping mapping) {
        return ClientVersionPatchMappingResponse.builder()
                .id(mapping.getId())
                .clientVersion(mapping.getClientVersion())
                .patchVersion(mapping.getPatchVersion())
                .createdAt(mapping.getCreatedAt())
                .updatedAt(mapping.getUpdatedAt())
                .build();
    }
}
