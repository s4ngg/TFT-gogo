package com.tftgogo.global.riot.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Map;
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

    /**
     * CDragon 실제 경로가 traitId 규칙과 다른 예외 목록.
     * 프론트 communityDragonAssets.ts의 TRAIT_ICON_PATHS와 동일하게 유지해야 함.
     * key: traitId 소문자, value: CDN_BASE_URL 이후 경로 (소문자)
     */
    private static final Map<String, String> TRAIT_ICON_OVERRIDES = Map.ofEntries(
        Map.entry("tft17_vanguard",   "/ux/traiticons/trait_icon_12_vanguard.tft_set12.png"),
        Map.entry("tft17_sniper",     "/ux/traiticons/trait_icon_6_sniper.png"),
        Map.entry("tft17_bastion",    "/ux/traiticons/trait_icon_9_bastion.png"),
        Map.entry("tft17_darkstar",   "/ux/traiticons/trait_icon_17_darkstar.tft_set17.png"),
        Map.entry("tft17_astronaut",  "/ux/traiticons/trait_icon_17_astronaut.tft_set17.png"),
        Map.entry("tft17_rogue",      "/ux/traiticons/trait_icon_17_rogue.tft_set17.png"),
        Map.entry("tft17_stargazer",  "/ux/traiticons/trait_icon_17_stargazer.tft_set17.png"),
        Map.entry("tft17_shepherd",   "/ux/traiticons/trait_icon_17_shepherd.tft_set17.png"),
        Map.entry("tft17_replicator", "/ux/traiticons/trait_icon_17_replicator.tft_set17.png"),
        Map.entry("tft17_psyops",     "/ux/traiticons/trait_icon_17_psyops.tft_set17.png")
    );

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

        String override = TRAIT_ICON_OVERRIDES.get(id);
        if (override != null) {
            return CDN_BASE_URL + override;
        }

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
