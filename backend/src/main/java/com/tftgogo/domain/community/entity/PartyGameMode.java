package com.tftgogo.domain.community.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PartyGameMode {
    RANKED_TFT("랭크"),
    NORMAL_TFT("일반"),
    CUSTOM("커스텀");

    private final String label;

    @JsonCreator
    public static PartyGameMode from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String normalizedValue = value.trim();
        return Arrays.stream(values())
                .filter(gameMode -> gameMode.name().equalsIgnoreCase(normalizedValue)
                        || gameMode.label.equalsIgnoreCase(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    }

    public static PartyGameMode fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return from(value);
    }

    @JsonValue
    public String getCode() {
        return name();
    }
}
