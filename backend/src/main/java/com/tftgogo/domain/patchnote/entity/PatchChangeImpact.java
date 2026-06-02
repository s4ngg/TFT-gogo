package com.tftgogo.domain.patchnote.entity;

import com.tftgogo.global.util.EnumParser;

public enum PatchChangeImpact {
    HIGH,
    MEDIUM,
    LOW;

    public static PatchChangeImpact from(String value) {
        return EnumParser.from(PatchChangeImpact.class, value);
    }
}
