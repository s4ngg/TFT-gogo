package com.tftgogo.global.riot.util;

import com.tftgogo.global.riot.config.TftAssetConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TftAssetUrlBuilder {

    private static final Logger logger = LogManager.getLogger(TftAssetUrlBuilder.class);

    private static final Pattern SET_PATTERN = Pattern.compile("(?:set|tft)(\\d+)");
    private static final Pattern TRAIT_SET_PATTERN = Pattern.compile("(?:set|tft)(\\d+)_(.+)");

    private static final Map<String, String> TRAIT_ICON_OVERRIDES = Map.ofEntries(
        Map.entry("tft17_vanguard",   traitIconPath(12, "vanguard")),
        Map.entry("tft17_sniper",     traitIconPathWithoutSet(6, "sniper")),
        Map.entry("tft17_bastion",    traitIconPathWithoutSet(9, "bastion")),
        Map.entry("tft17_darkstar",   traitIconPath(17, "darkstar")),
        Map.entry("tft17_astronaut",  traitIconPath(17, "astronaut")),
        Map.entry("tft17_rogue",      traitIconPath(17, "rogue")),
        Map.entry("tft17_stargazer",  traitIconPath(17, "stargazer")),
        Map.entry("tft17_shepherd",   traitIconPath(17, "shepherd")),
        Map.entry("tft17_replicator", traitIconPath(17, "replicator")),
        Map.entry("tft17_psyops",     traitIconPath(17, "psyops"))
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
        return TftAssetConfig.CDRAGON_ASSET_BASE_URL + "/characters/" + id + "/hud/" + id + "_square." + setTag + ".png";
    }

    private static String championImageOverride(String id) {
        return switch (id) {
            case "tft17_rhaast" -> TftAssetConfig.ddragonTftChampionImageUrl(
                    "TFT17_KaynSplash_Uncentered." + TftAssetConfig.setFileSuffix(17) + ".png");
            default -> null;
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
            return TftAssetConfig.CDRAGON_ASSET_BASE_URL + override;
        }

        Matcher matcher = TRAIT_SET_PATTERN.matcher(id);
        if (matcher.matches()) {
            int setNumber = Integer.parseInt(matcher.group(1));
            String traitName = matcher.group(2);
            return TftAssetConfig.CDRAGON_ASSET_BASE_URL + traitIconPath(setNumber, traitName);
        }

        String fallbackUrl = TftAssetConfig.CDRAGON_ASSET_BASE_URL + "/ux/traiticons/" + id + ".png";
        logger.warn("Trait ID pattern did not match; using fallback trait icon URL. traitId={}, pattern={}, fallbackUrl={}",
                traitId, TRAIT_SET_PATTERN.pattern(), fallbackUrl);
        return fallbackUrl;
    }

    private static String extractSetTag(String id) {
        Matcher matcher = SET_PATTERN.matcher(id);
        return matcher.find()
                ? TftAssetConfig.setTag(Integer.parseInt(matcher.group(1)))
                : TftAssetConfig.CURRENT_SET_TAG;
    }

    public static String buildItemIconUrl(String itemId) {
        Objects.requireNonNull(itemId, "TftAssetUrlBuilder: itemId must not be null");
        if (itemId.isBlank()) {
            throw new IllegalStateException("TftAssetUrlBuilder: itemId must not be blank");
        }
        String id = itemId.toLowerCase(Locale.ROOT);
        String fallbackUrl = TftAssetConfig.CDRAGON_ASSET_BASE_URL + "/maps/particles/tft/item_icons/standard/" + id + ".png";
        logger.warn("Item icon cache miss; using fallback URL. itemId={}, fallbackUrl={}", itemId, fallbackUrl);
        return fallbackUrl;
    }

    private static String traitIconPath(int iconSetNumber, String traitName) {
        return "/ux/traiticons/trait_icon_" + iconSetNumber + "_" + traitName
                + "." + TftAssetConfig.setTag(iconSetNumber) + ".png";
    }

    private static String traitIconPathWithoutSet(int iconSetNumber, String traitName) {
        return "/ux/traiticons/trait_icon_" + iconSetNumber + "_" + traitName + ".png";
    }
}
