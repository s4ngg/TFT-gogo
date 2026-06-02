package com.tftgogo.domain.patchnote.entity;

import com.tftgogo.global.util.EnumParser;

public enum PatchChangeCategory {
    CHAMPION,
    TRAIT,
    ITEM,
    AUGMENT,
    SYSTEM;

    public static PatchChangeCategory from(String value) {
        return EnumParser.from(PatchChangeCategory.class, value);
    }
}
