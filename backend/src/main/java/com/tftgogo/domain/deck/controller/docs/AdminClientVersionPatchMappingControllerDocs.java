package com.tftgogo.domain.deck.controller.docs;

import com.tftgogo.domain.deck.dto.request.AdminClientVersionPatchMappingRequest;
import com.tftgogo.domain.deck.dto.response.ClientVersionPatchMappingResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Admin Client Version Patch Mappings", description = "Admin client build version to TFT patch version mapping APIs")
public interface AdminClientVersionPatchMappingControllerDocs {

    @Operation(summary = "List client version patch mappings", description = "Lists all client build version to TFT patch version mappings.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed")
    })
    ResponseEntity<ApiResponse<List<ClientVersionPatchMappingResponse>>> getMappings();

    @Operation(summary = "Create client version patch mapping", description = "Registers a client build version to TFT patch version mapping and backfills existing meta deck aggregates.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Client version already mapped")
    })
    ResponseEntity<ApiResponse<ClientVersionPatchMappingResponse>> createMapping(
            @Valid @RequestBody AdminClientVersionPatchMappingRequest request
    );

    @Operation(summary = "Update client version patch mapping", description = "Updates a client build version to TFT patch version mapping and backfills existing meta deck aggregates.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mapping not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Client version already mapped")
    })
    ResponseEntity<ApiResponse<ClientVersionPatchMappingResponse>> updateMapping(
            @PathVariable("mappingId") Long mappingId,
            @Valid @RequestBody AdminClientVersionPatchMappingRequest request
    );

    @Operation(summary = "Delete client version patch mapping", description = "Deletes a client build version to TFT patch version mapping.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mapping not found")
    })
    ResponseEntity<ApiResponse<Void>> deleteMapping(@PathVariable("mappingId") Long mappingId);
}
