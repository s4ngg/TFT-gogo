package com.tftgogo.domain.content.controller.docs;

import com.tftgogo.domain.content.dto.response.ContentRefreshHealthResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin Content Refresh", description = "Admin content refresh monitoring APIs")
public interface AdminContentRefreshControllerDocs {

    @Operation(
            summary = "Get content refresh health",
            description = "Returns persisted scheduler run state and current patch-note/guide data health."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    ResponseEntity<ApiResponse<ContentRefreshHealthResponse>> getHealth();
}
