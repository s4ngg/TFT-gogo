package com.tftgogo.global.riot.util;

import com.tftgogo.global.riot.config.TftAssetConfig;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class TftShopUnitFilterTest {

    @Test
    void 현재_시즌_상점_유닛이면_true를_반환한다() {
        String currentSetJinx = TftAssetConfig.setUnitIdPrefix(TftAssetConfig.CURRENT_SET_NUMBER) + "jinx";

        assertThat(TftShopUnitFilter.isShopUnit(currentSetJinx)).isTrue();
    }

    @Test
    void characterId_대소문자가_달라도_상점_유닛이면_true를_반환한다() {
        String currentSetAhri = TftAssetConfig.setUnitIdPrefix(TftAssetConfig.CURRENT_SET_NUMBER)
                .toUpperCase(Locale.ROOT) + "AHRI";

        assertThat(TftShopUnitFilter.isShopUnit(currentSetAhri)).isTrue();
    }

    @Test
    void 다른_시즌_유닛이면_false를_반환한다() {
        int otherSetNumber = TftAssetConfig.CURRENT_SET_NUMBER + 1;
        String otherSetJinx = TftAssetConfig.setUnitIdPrefix(otherSetNumber) + "jinx";

        assertThat(TftShopUnitFilter.isShopUnit(otherSetJinx)).isFalse();
    }

    @Test
    void 비상점_유닛이면_false를_반환한다() {
        String currentSetDummy = TftAssetConfig.setUnitIdPrefix(TftAssetConfig.CURRENT_SET_NUMBER) + "trainingdummy";

        assertThat(TftShopUnitFilter.isShopUnit(currentSetDummy)).isFalse();
    }

    @Test
    void characterId가_null_또는_blank이면_false를_반환한다() {
        assertThat(TftShopUnitFilter.isShopUnit(null)).isFalse();
        assertThat(TftShopUnitFilter.isShopUnit(" ")).isFalse();
    }
}
