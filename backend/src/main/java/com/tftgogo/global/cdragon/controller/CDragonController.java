package com.tftgogo.global.cdragon.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tftgogo.global.cdragon.controller.docs.CDragonControllerDocs;
import com.tftgogo.global.cdragon.service.TftAssetCacheService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cdragon")
@RequiredArgsConstructor
public class CDragonController implements CDragonControllerDocs {

    private final TftAssetCacheService tftAssetCacheService;

    @Override
    @GetMapping("/tft/ko-kr")
    public ResponseEntity<ApiResponse<JsonNode>> getTftKoKrLocale() {
        JsonNode locale = tftAssetCacheService.getTftKoKrLocale();

        return ResponseEntity.ok(ApiResponse.success("CDragon TFT locale lookup success", locale));
    }
}
