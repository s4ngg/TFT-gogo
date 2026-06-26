package com.tftgogo.global.cdragon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.global.cdragon.config.CommunityDragonProperties;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.config.TftAssetConfig;
import com.tftgogo.global.riot.util.TftAssetUrlBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TftAssetCacheService {

    private static final Logger logger = LogManager.getLogger(TftAssetCacheService.class);

    private final RestTemplate restTemplate;
    private final CommunityDragonProperties communityDragonProperties;
    private final ObjectMapper objectMapper;

    private volatile Map<String, String> traitIconCache = Map.of();
    private volatile Map<String, String> traitNameCache = Map.of();
    private volatile Map<String, String> itemIconCache = Map.of();
    private volatile JsonNode tftKoKrLocaleCache;

    @PostConstruct
    public void init() {
        try {
            JsonNode root = fetchTftKoKrLocaleFromSource();
            tftKoKrLocaleCache = root;
            traitIconCache = buildTraitIconCache(root);
            traitNameCache = buildTraitNameCache(root);
            itemIconCache = buildItemIconCache(root);
            logger.info(
                    "TFT asset cache loaded: traits={}, items={}",
                    traitIconCache.size(),
                    itemIconCache.size()
            );
        } catch (Exception e) {
            logger.warn(
                    "Failed to load CDragon TFT asset cache. Falling back to asset URL builder. error={}",
                    e.getMessage()
            );
        }
    }

    public String getTraitIconUrl(String traitId) {
        String url = traitIconCache.get(traitId.toLowerCase(Locale.ROOT));
        if (url != null) return url;
        return TftAssetUrlBuilder.buildTraitIconUrl(traitId);
    }

    public String getTraitName(String traitId) {
        String name = traitNameCache.get(traitId.toLowerCase(Locale.ROOT));
        return name != null ? name : traitId;
    }

    public String getItemIconUrl(String itemId) {
        String url = itemIconCache.get(itemId.toLowerCase(Locale.ROOT));
        if (url != null) return url;
        return TftAssetUrlBuilder.buildItemIconUrl(itemId);
    }

    public JsonNode getTftKoKrLocale() {
        JsonNode cachedLocale = tftKoKrLocaleCache;
        if (cachedLocale != null) {
            return cachedLocale;
        }

        synchronized (this) {
            cachedLocale = tftKoKrLocaleCache;
            if (cachedLocale != null) {
                return cachedLocale;
            }

            try {
                JsonNode locale = fetchTftKoKrLocaleFromSource();
                tftKoKrLocaleCache = locale;
                return locale;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Failed to fetch CDragon TFT locale. error={}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
            }
        }
    }

    private JsonNode fetchTftKoKrLocaleFromSource() throws Exception {
        String response = restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class);
        if (response == null || response.isBlank()) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
        return objectMapper.readTree(response);
    }

    private Map<String, String> buildTraitIconCache(JsonNode root) {
        Map<String, String> cache = new HashMap<>();
        for (JsonNode setData : root.path("setData")) {
            if (setData.path("number").asInt() != TftAssetConfig.CURRENT_SET_NUMBER) continue;
            for (JsonNode trait : setData.path("traits")) {
                String apiName = trait.path("apiName").asText();
                String icon = trait.path("icon").asText();
                if (!apiName.isBlank() && !icon.isBlank()) {
                    cache.put(apiName.toLowerCase(Locale.ROOT), assetUrl(icon));
                }
            }
        }
        return cache;
    }

    private Map<String, String> buildTraitNameCache(JsonNode root) {
        Map<String, String> cache = new HashMap<>();
        for (JsonNode setData : root.path("setData")) {
            if (setData.path("number").asInt() != TftAssetConfig.CURRENT_SET_NUMBER) continue;
            for (JsonNode trait : setData.path("traits")) {
                String apiName = trait.path("apiName").asText();
                String name = trait.path("name").asText();
                if (!apiName.isBlank() && !name.isBlank()) {
                    cache.put(apiName.toLowerCase(Locale.ROOT), name);
                }
            }
        }
        return cache;
    }

    private Map<String, String> buildItemIconCache(JsonNode root) {
        Map<String, String> cache = new HashMap<>();
        for (JsonNode item : root.path("items")) {
            String apiName = item.path("apiName").asText();
            String icon = item.path("icon").asText();
            if (!apiName.isBlank() && !icon.isBlank()) {
                cache.put(apiName.toLowerCase(Locale.ROOT), assetUrl(icon));
            }
        }
        return cache;
    }

    private String assetUrl(String assetPath) {
        return communityDragonProperties.getAssetBaseUrl()
                + "/"
                + assetPath.toLowerCase(Locale.ROOT).replace(".tex", ".png");
    }
}
