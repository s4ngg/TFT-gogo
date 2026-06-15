package com.tftgogo.global.riot.util;

import com.tftgogo.global.riot.config.TftAssetConfig;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class TftShopUnitFilter {

    // Unit names are season roster data; the TFT set prefix follows TftAssetConfig.
    private static final String SHOP_UNIT_PREFIX = TftAssetConfig.setUnitIdPrefix(
            TftAssetConfig.CURRENT_SET_NUMBER
    );
    private static final Set<String> SHOP_UNIT_NAMES = Set.of(
            "aatrox",
            "ahri",
            "akali",
            "aurelionsol",
            "aurora",
            "bard",
            "belveth",
            "blitzcrank",
            "briar",
            "caitlyn",
            "chogath",
            "corki",
            "diana",
            "ezreal",
            "fiora",
            "fizz",
            "galio",
            "gnar",
            "gragas",
            "graves",
            "gwen",
            "illaoi",
            "jax",
            "jhin",
            "jinx",
            "kaisa",
            "karma",
            "kindred",
            "leblanc",
            "leona",
            "lissandra",
            "lulu",
            "maokai",
            "masteryi",
            "milio",
            "missfortune",
            "mordekaiser",
            "morgana",
            "nunu",
            "ornn",
            "pantheon",
            "poppy",
            "rammus",
            "reksai",
            "rhaast",
            "riven",
            "samira",
            "shen",
            "sona",
            "tahmkench",
            "talon",
            "teemo",
            "twistedfate",
            "urgot",
            "vex",
            "viktor",
            "xayah",
            "zoe"
    );
    private static final Set<String> SHOP_UNIT_IDS = SHOP_UNIT_NAMES.stream()
            .map(unitName -> SHOP_UNIT_PREFIX + unitName)
            .collect(Collectors.toUnmodifiableSet());

    private TftShopUnitFilter() {
    }

    public static boolean isShopUnit(String characterId) {
        if (characterId == null || characterId.isBlank()) {
            return false;
        }

        return SHOP_UNIT_IDS.contains(characterId.toLowerCase(Locale.ROOT));
    }
}
