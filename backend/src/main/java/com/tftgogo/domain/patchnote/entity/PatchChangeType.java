package com.tftgogo.domain.patchnote.entity;

import com.tftgogo.global.util.EnumParser;

public enum PatchChangeType {
    BUFF,
    NERF,
    ADJUST,
    NEW;

    public static PatchChangeType from(String value) {
        return EnumParser.from(PatchChangeType.class, value);
    }
}
