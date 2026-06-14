package com.tftgogo.global.cdragon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.global.cdragon.config.CommunityDragonProperties;
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

    @PostConstruct
    public void init() {
        try {
            String response = restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class);
            if (response == null || response.isBlank()) {
                logger.warn("CDragon 응답이 비어있습니다. 기본 URL 빌더로 폴백합니다.");
                return;
            }
            JsonNode root = objectMapper.readTree(response);
            traitIconCache = buildTraitIconCache(root);
            traitNameCache = buildTraitNameCache(root);
            itemIconCache = buildItemIconCache(root);
            logger.info("TFT 에셋 캐시 로드 완료: 시너지 {}개, 아이템 {}개", traitIconCache.size(), itemIconCache.size());
        } catch (Exception e) {
            logger.warn("CDragon에서 TFT 에셋 캐시를 로드하지 못했습니다. 기본 URL 빌더로 폴백합니다. error={}", e.getMessage());
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
        return itemIconCache.get(itemId.toLowerCase(Locale.ROOT));
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
