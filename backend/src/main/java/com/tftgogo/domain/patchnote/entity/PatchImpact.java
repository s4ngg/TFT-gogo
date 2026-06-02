package com.tftgogo.domain.patchnote.entity;

import com.tftgogo.global.util.EnumParser;

public enum PatchImpact {
    HIGH,
    MEDIUM,
    LOW;

    public static PatchImpact from(String value) {
        return EnumParser.from(PatchImpact.class, value);
    }
}
