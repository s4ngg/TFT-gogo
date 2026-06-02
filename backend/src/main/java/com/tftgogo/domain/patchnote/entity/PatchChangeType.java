package com.tftgogo.domain.patchnote.entity;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;

import java.util.Locale;

public enum PatchChangeType {
    BUFF,
    NERF,
    ADJUST,
    NEW;

    public static PatchChangeType from(String value) {
        try {
            return PatchChangeType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
