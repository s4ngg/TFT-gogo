package com.tftgogo.global.riot.util;

import java.util.Locale;
import java.util.Set;

public final class TftShopUnitFilter {

    // TODO: 시즌 변경 시 수동 업데이트 필요 — 현재 TFT 시즌 17(tft17_) 기준
    //       새 시즌 출시 시 접두사(tft17_ → tft18_ 등)와 유닛 목록을 함께 갱신할 것
    private static final Set<String> SHOP_UNIT_IDS = Set.of(
            "tft17_aatrox",
            "tft17_ahri",
            "tft17_akali",
            "tft17_aurelionsol",
            "tft17_aurora",
            "tft17_bard",
            "tft17_belveth",
            "tft17_blitzcrank",
            "tft17_briar",
            "tft17_caitlyn",
            "tft17_chogath",
            "tft17_corki",
            "tft17_diana",
            "tft17_ezreal",
            "tft17_fiora",
            "tft17_fizz",
            "tft17_galio",
            "tft17_gnar",
            "tft17_gragas",
            "tft17_graves",
            "tft17_gwen",
            "tft17_illaoi",
            "tft17_jax",
            "tft17_jhin",
            "tft17_jinx",
            "tft17_kaisa",
            "tft17_karma",
            "tft17_kindred",
            "tft17_leblanc",
            "tft17_leona",
            "tft17_lissandra",
            "tft17_lulu",
            "tft17_maokai",
            "tft17_masteryi",
            "tft17_milio",
            "tft17_missfortune",
            "tft17_mordekaiser",
            "tft17_morgana",
            "tft17_nunu",
            "tft17_ornn",
            "tft17_pantheon",
            "tft17_poppy",
            "tft17_rammus",
            "tft17_reksai",
            "tft17_rhaast",
            "tft17_riven",
            "tft17_samira",
            "tft17_shen",
            "tft17_sona",
            "tft17_tahmkench",
            "tft17_talon",
            "tft17_teemo",
            "tft17_twistedfate",
            "tft17_urgot",
            "tft17_vex",
            "tft17_viktor",
            "tft17_xayah",
            "tft17_zoe"
    );

    private TftShopUnitFilter() {
    }

    public static boolean isShopUnit(String characterId) {
        if (characterId == null || characterId.isBlank()) {
            return false;
        }

        return SHOP_UNIT_IDS.contains(characterId.toLowerCase(Locale.ROOT));
    }
}
