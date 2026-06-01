package com.tftgogo.global.riot.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TftAssetUrlBuilder {

    private static final Logger logger = LogManager.getLogger(TftAssetUrlBuilder.class);

    private static final String CDN_BASE_URL = "https://raw.communitydragon.org/latest/game/assets";
    private static final String DEFAULT_SET_TAG = "tft_set17";
    // TODO: 시즌 업데이트 시 DDragon 버전 갱신 필요 (championImageOverride에서만 사용)
    private static final String DDRAGON_VERSION = "16.10.1";
    private static final Pattern SET_PATTERN = Pattern.compile("(?:set|tft)(\\d+)");
    private static final Pattern TRAIT_SET_PATTERN = Pattern.compile("(?:set|tft)(\\d+)_(.+)");

    private TftAssetUrlBuilder() {
    }

    public static String buildChampionImageUrl(String characterId) {
        Objects.requireNonNull(characterId, "TftAssetUrlBuilder: characterId must not be null");
        if (characterId.isBlank()) {
            throw new IllegalStateException("TftAssetUrlBuilder: characterId must not be blank");
        }

        String id = characterId.toLowerCase(Locale.ROOT);
        String overrideUrl = championImageOverride(id);
        if (overrideUrl != null) {
            return overrideUrl;
        }

        String setTag = extractSetTag(id);
        return CDN_BASE_URL + "/characters/" + id + "/hud/" + id + "_square." + setTag + ".png";
    }

    private static String championImageOverride(String id) {
        return switch (id) {
            case "tft17_rhaast" -> "https://ddragon.leagueoflegends.com/cdn/" + DDRAGON_VERSION
                    + "/img/tft-champion/TFT17_KaynSplash_Uncentered.TFT_Set17.png";
            default -> null;  // null = 오버라이드 없음, 호출부에서 CDragon URL 생성
        };
    }

    public static String buildTraitIconUrl(String traitId) {
        Objects.requireNonNull(traitId, "TftAssetUrlBuilder: traitId must not be null");
        if (traitId.isBlank()) {
            throw new IllegalStateException("TftAssetUrlBuilder: traitId must not be blank");
        }

        String id = traitId.toLowerCase(Locale.ROOT);
        Matcher matcher = TRAIT_SET_PATTERN.matcher(id);
        if (matcher.matches()) {
            String setNum = matcher.group(1);
            String traitName = matcher.group(2);
            return CDN_BASE_URL + "/ux/traiticons/trait_icon_"
                    + setNum + "_" + traitName + ".tft_set" + setNum + ".png";
        }

        String fallbackUrl = CDN_BASE_URL + "/ux/traiticons/" + id + ".png";
        logger.warn("Trait ID pattern did not match; using fallback trait icon URL. traitId={}, pattern={}, fallbackUrl={}",
                traitId, TRAIT_SET_PATTERN.pattern(), fallbackUrl);
        return fallbackUrl;
    }

    private static String extractSetTag(String id) {
        Matcher matcher = SET_PATTERN.matcher(id);
        return matcher.find() ? "tft_set" + matcher.group(1) : DEFAULT_SET_TAG;
    }
}
