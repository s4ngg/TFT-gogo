package com.tftgogo.global.cdragon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.global.cdragon.config.CommunityDragonProperties;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TftAssetCacheServiceTest {

    private static final String CDRAGON_URL = "https://example.com/cdragon/tft/ko_kr.json";

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private CommunityDragonProperties communityDragonProperties = new CommunityDragonProperties();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TftAssetCacheService tftAssetCacheService;

    @BeforeEach
    void setUp() {
        communityDragonProperties.setTftKoKrUrl(CDRAGON_URL);
        communityDragonProperties.setAssetBaseUrl("https://raw.communitydragon.org/latest/game");
    }

    @Test
    void cdragon_locale_is_reused_after_startup_cache_load() {
        // given
        when(restTemplate.getForObject(CDRAGON_URL, String.class)).thenReturn(cdragonJson());

        // when
        tftAssetCacheService.init();
        JsonNode first = tftAssetCacheService.getTftKoKrLocale();
        JsonNode second = tftAssetCacheService.getTftKoKrLocale();

        // then
        assertThat(first.path("items").get(0).path("apiName").asText()).isEqualTo("TFT_Item_Test");
        assertThat(second).isSameAs(first);
        verify(restTemplate, times(1)).getForObject(CDRAGON_URL, String.class);
    }

    @Test
    void cdragon_locale_cache_miss_fetches_once_and_reuses_response() {
        // given
        when(restTemplate.getForObject(CDRAGON_URL, String.class)).thenReturn(cdragonJson());

        // when
        JsonNode first = tftAssetCacheService.getTftKoKrLocale();
        JsonNode second = tftAssetCacheService.getTftKoKrLocale();

        // then
        assertThat(first.path("setData").get(0).path("traits").get(0).path("name").asText()).isEqualTo("Test Trait");
        assertThat(second).isSameAs(first);
        assertThat(tftAssetCacheService.getTraitName("TFT17_TestTrait")).isEqualTo("Test Trait");
        assertThat(tftAssetCacheService.getItemIconUrl("TFT_Item_Test"))
                .isEqualTo("https://raw.communitydragon.org/latest/game/assets/maps/particles/tft/item_test.png");
        verify(restTemplate, times(1)).getForObject(CDRAGON_URL, String.class);
    }

    @Test
    void cdragon_locale_empty_response_throws_external_api_error() {
        // given
        when(restTemplate.getForObject(CDRAGON_URL, String.class)).thenReturn("");

        // when & then
        assertThatThrownBy(() -> tftAssetCacheService.getTftKoKrLocale())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
    }

    @Test
    void cdragon_locale_rest_failure_throws_external_api_error() {
        // given
        when(restTemplate.getForObject(CDRAGON_URL, String.class))
                .thenThrow(new RestClientException("Connection timeout"));

        // when & then
        assertThatThrownBy(() -> tftAssetCacheService.getTftKoKrLocale())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
    }

    @Test
    void cdragon_locale_parse_failure_throws_external_api_error() {
        // given
        when(restTemplate.getForObject(CDRAGON_URL, String.class)).thenReturn("invalid-json-{{{");

        // when & then
        assertThatThrownBy(() -> tftAssetCacheService.getTftKoKrLocale())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
    }

    private String cdragonJson() {
        return """
                {
                  "items": [
                    {
                      "apiName": "TFT_Item_Test",
                      "icon": "ASSETS/Maps/Particles/TFT/Item_Test.tex"
                    }
                  ],
                  "setData": [
                    {
                      "number": 17,
                      "traits": [
                        {
                          "apiName": "TFT17_TestTrait",
                          "name": "Test Trait",
                          "icon": "ASSETS/UX/TraitIcons/Trait_Icon_Test.tex"
                        }
                      ]
                    }
                  ]
                }
                """;
    }
}
