package com.tftgogo.domain.patchnote.entity;

import com.tftgogo.global.util.EnumParser;

public enum PatchCategory {
    CHAMPION,
    TRAIT,
    ITEM,
    AUGMENT,
    SYSTEM;

    public static PatchCategory from(String value) {
        return EnumParser.from(PatchCategory.class, value);
    }
}
