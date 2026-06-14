package com.tftgogo.global.riot.config;

public final class TftAssetConfig {

    public static final int CURRENT_SET_NUMBER = 17;
    public static final String CURRENT_SET_TAG = setTag(CURRENT_SET_NUMBER);
    public static final String CDRAGON_ASSET_BASE_URL = "https://raw.communitydragon.org/latest/game/assets";
    public static final String DDRAGON_CDN_BASE_URL = "https://ddragon.leagueoflegends.com/cdn";
    public static final String DDRAGON_VERSION = "16.10.1";

    private TftAssetConfig() {
    }

    public static String setTag(int setNumber) {
        validateSetNumber(setNumber);
        return "tft_set" + setNumber;
    }

    public static String setFileSuffix(int setNumber) {
        validateSetNumber(setNumber);
        return "TFT_Set" + setNumber;
    }

    public static String setUnitIdPrefix(int setNumber) {
        validateSetNumber(setNumber);
        return "tft" + setNumber + "_";
    }

    public static String ddragonTftChampionImageUrl(String fileName) {
        return DDRAGON_CDN_BASE_URL + "/" + DDRAGON_VERSION + "/img/tft-champion/" + fileName;
    }

    private static void validateSetNumber(int setNumber) {
        if (setNumber <= 0) {
            throw new IllegalArgumentException("TFT set number must be positive");
        }
    }
}
