package com.tftgogo.domain.patchnote.service;

import com.tftgogo.domain.patchnote.dto.request.AdminPatchChangeRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteImportRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteRequest;
import com.tftgogo.domain.patchnote.dto.response.AdminPatchNoteImportResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;

import java.util.List;

public interface AdminPatchNoteService {

    List<PatchNoteResponse> getPatchNotes();

    PatchNoteResponse createPatchNote(AdminPatchNoteRequest request);

    AdminPatchNoteImportResponse importRiotPatchNote(AdminPatchNoteImportRequest request);

    PatchNoteResponse updatePatchNote(Long patchNoteId, AdminPatchNoteRequest request);

    void deletePatchNote(Long patchNoteId);

    List<PatchChangeResponse> getPatchChanges(Long patchNoteId);

    PatchChangeResponse createPatchChange(AdminPatchChangeRequest request);

    PatchChangeResponse updatePatchChange(Long changeId, AdminPatchChangeRequest request);

    void deletePatchChange(Long changeId);
}
