package com.tftgogo.global.riot.util;

import com.tftgogo.global.riot.config.TftAssetConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TftAssetUrlBuilderTest {

    private static final String CDN_BASE = TftAssetConfig.CDRAGON_ASSET_BASE_URL;

    @ParameterizedTest(name = "{0} -> {1}")
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
    void 검증된_traitId는_override_CDragon_경로를_반환한다(String traitId, String expectedPath) {
        String url = TftAssetUrlBuilder.buildTraitIconUrl(traitId.trim());
        assertThat(url).isEqualTo(CDN_BASE + expectedPath.trim());
    }

    @Test
    void 일반_traitId는_set번호_규칙으로_URL을_생성한다() {
        String url = TftAssetUrlBuilder.buildTraitIconUrl("TFT17_Bruiser");
        assertThat(url).isEqualTo(CDN_BASE + "/ux/traiticons/trait_icon_17_bruiser.tft_set17.png");
    }

    @Test
    void traitId의_set번호를_기준으로_URL을_생성한다() {
        String url = TftAssetUrlBuilder.buildTraitIconUrl("TFT18_Bruiser");
        assertThat(url).isEqualTo(CDN_BASE + "/ux/traiticons/trait_icon_18_bruiser.tft_set18.png");
    }

    @Test
    void championId의_set번호를_기준으로_square_URL을_생성한다() {
        String url = TftAssetUrlBuilder.buildChampionImageUrl("TFT18_Ahri");
        assertThat(url).isEqualTo(CDN_BASE + "/characters/tft18_ahri/hud/tft18_ahri_square.tft_set18.png");
    }

    @Test
    void championId의_set번호가_없으면_현재_시즌_setTag를_fallback으로_사용한다() {
        String url = TftAssetUrlBuilder.buildChampionImageUrl("TFT_ItemDummy");
        assertThat(url).isEqualTo(CDN_BASE + "/characters/tft_itemdummy/hud/tft_itemdummy_square."
                + TftAssetConfig.CURRENT_SET_TAG + ".png");
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
