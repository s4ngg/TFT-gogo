package com.tftgogo.domain.patchnote.entity;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;

import java.util.Locale;

public enum PatchCategory {
    CHAMPION,
    TRAIT,
    ITEM,
    AUGMENT,
    SYSTEM;

    public static PatchCategory from(String value) {
        try {
            return PatchCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
