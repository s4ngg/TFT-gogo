package com.tftgogo.global.riot.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TftAssetUrlBuilderTest {

    private static final String CDN_BASE = "https://raw.communitydragon.org/latest/game/assets";

    // ── 예외 경로 (프론트 TRAIT_ICON_PATHS와 동일해야 함) ────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "TFT17_Vanguard,   /ux/traiticons/trait_icon_12_vanguard.tft_set12.png",
        "TFT17_Sniper,     /ux/traiticons/trait_icon_6_sniper.png",
        "TFT17_Bastion,    /ux/traiticons/trait_icon_9_bastion.png",
        "TFT17_DarkStar,   /ux/traiticons/trait_icon_17_darkstar.tft_set17.png",
        "TFT17_Astronaut,  /ux/traiticons/trait_icon_17_astronaut.tft_set17.png",
        "TFT17_Rogue,      /ux/traiticons/trait_icon_17_rogue.tft_set17.png",
        "TFT17_Stargazer,  /ux/traiticons/trait_icon_17_stargazer.tft_set17.png",
        "TFT17_Shepherd,   /ux/traiticons/trait_icon_17_shepherd.tft_set17.png",
        "TFT17_Replicator, /ux/traiticons/trait_icon_17_replicator.tft_set17.png",
        "TFT17_PsyOps,     /ux/traiticons/trait_icon_17_psyops.tft_set17.png",
    })
    void 예외_traitId는_검증된_CDragon_경로를_반환한다(String traitId, String expectedPath) {
        String url = TftAssetUrlBuilder.buildTraitIconUrl(traitId.trim());
        assertThat(url).isEqualTo(CDN_BASE + expectedPath.trim());
    }

    @Test
    void 일반_traitId는_set번호_규칙으로_URL을_생성한다() {
        String url = TftAssetUrlBuilder.buildTraitIconUrl("TFT17_Bruiser");
        assertThat(url).isEqualTo(CDN_BASE + "/ux/traiticons/trait_icon_17_bruiser.tft_set17.png");
    }

    @Test
    void traitId가_null이면_예외를_던진다() {
        assertThatThrownBy(() -> TftAssetUrlBuilder.buildTraitIconUrl(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void traitId가_blank이면_예외를_던진다() {
        assertThatThrownBy(() -> TftAssetUrlBuilder.buildTraitIconUrl("  "))
                .isInstanceOf(IllegalStateException.class);
    }
}
