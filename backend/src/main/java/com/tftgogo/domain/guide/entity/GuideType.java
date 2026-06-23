package com.tftgogo.domain.guide.entity;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;

public enum GuideType {
    TRAIT("traits"),
    ITEM("items"),
    AUGMENT("augments"),
    CHAMPION("champions");

    private final String tab;

    GuideType(String tab) {
        this.tab = tab;
    }

    public String getTab() {
        return tab;
    }

    public static GuideType fromTab(String tab) {
        for (GuideType guideType : values()) {
            if (guideType.tab.equals(tab)) {
                return guideType;
            }
        }
        throw new BusinessException(ErrorCode.GUIDE_INVALID_TAB);
    }
}
