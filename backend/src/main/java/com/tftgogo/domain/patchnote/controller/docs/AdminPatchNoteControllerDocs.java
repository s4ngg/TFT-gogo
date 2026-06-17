package com.tftgogo.domain.patchnote.controller.docs;

import com.tftgogo.domain.patchnote.dto.request.AdminPatchChangeRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteImportRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteRequest;
import com.tftgogo.domain.patchnote.dto.response.AdminPatchNoteImportResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Admin PatchNotes", description = "Admin patch note APIs")
public interface AdminPatchNoteControllerDocs {

    @Operation(summary = "List admin patch notes", description = "Lists non-deleted patch notes for admin management.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed")
    })
    ResponseEntity<ApiResponse<List<PatchNoteResponse>>> getPatchNotes();

    @Operation(summary = "Create patch note", description = "Creates a patch note manually.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed")
    })
    ResponseEntity<ApiResponse<PatchNoteResponse>> createPatchNote(
            @Valid @RequestBody AdminPatchNoteRequest request
    );

    @Operation(summary = "Import Riot official patch note", description = "Fetches an official Riot patch note and stores it as patch note data.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed")
    })
    ResponseEntity<ApiResponse<AdminPatchNoteImportResponse>> importRiotPatchNote(
            @Valid @RequestBody(required = false) AdminPatchNoteImportRequest request
    );

    @Operation(summary = "Update patch note", description = "Updates a patch note manually.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Patch note not found")
    })
    ResponseEntity<ApiResponse<PatchNoteResponse>> updatePatchNote(
            @PathVariable("patchNoteId") Long patchNoteId,
            @Valid @RequestBody AdminPatchNoteRequest request
    );

    @Operation(summary = "Delete patch note", description = "Soft-deletes a patch note.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Patch note not found")
    })
    ResponseEntity<ApiResponse<Void>> deletePatchNote(
            @PathVariable("patchNoteId") Long patchNoteId
    );

    @Operation(summary = "List admin patch changes", description = "Lists patch changes for a patch note.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Patch note not found")
    })
    ResponseEntity<ApiResponse<List<PatchChangeResponse>>> getPatchChanges(
            @PathVariable("patchNoteId") Long patchNoteId
    );

    @Operation(summary = "Create patch change", description = "Creates a patch change manually.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Patch note not found")
    })
    ResponseEntity<ApiResponse<PatchChangeResponse>> createPatchChange(
            @Valid @RequestBody AdminPatchChangeRequest request
    );

    @Operation(summary = "Update patch change", description = "Updates a patch change manually.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Patch note or change not found")
    })
    ResponseEntity<ApiResponse<PatchChangeResponse>> updatePatchChange(
            @PathVariable("changeId") Long changeId,
            @Valid @RequestBody AdminPatchChangeRequest request
    );

    @Operation(summary = "Delete patch change", description = "Deletes a patch change.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Admin authentication failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Patch change not found")
    })
    ResponseEntity<ApiResponse<Void>> deletePatchChange(
            @PathVariable("changeId") Long changeId
    );
}
