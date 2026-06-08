package com.tftgogo.domain.patchnote.service;

import com.tftgogo.domain.patchnote.dto.request.AdminPatchChangeRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteRequest;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;

import java.util.List;

public interface AdminPatchNoteService {

    List<PatchNoteResponse> getPatchNotes();

    PatchNoteResponse createPatchNote(AdminPatchNoteRequest request);

    PatchNoteResponse updatePatchNote(Long patchNoteId, AdminPatchNoteRequest request);

    void deletePatchNote(Long patchNoteId);

    PatchChangeResponse createPatchChange(AdminPatchChangeRequest request);

    PatchChangeResponse updatePatchChange(Long changeId, AdminPatchChangeRequest request);

    void deletePatchChange(Long changeId);
}
