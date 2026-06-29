package com.tftgogo.global.cdragon.controller.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Community Dragon", description = "Community Dragon data proxy API")
public interface CDragonControllerDocs {

    @Operation(
            summary = "TFT ko-KR locale data proxy",
            description = "Returns CDragon TFT ko-KR data through the backend to avoid browser CORS restrictions."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success")
    })
    ResponseEntity<ApiResponse<JsonNode>> getTftKoKrLocale();
}
