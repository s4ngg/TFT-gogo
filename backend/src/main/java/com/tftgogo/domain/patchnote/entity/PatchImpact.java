package com.tftgogo.domain.patchnote.entity;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;

import java.util.Locale;

public enum PatchImpact {
    HIGH,
    MEDIUM,
    LOW;

    public static PatchImpact from(String value) {
        try {
            return PatchImpact.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
