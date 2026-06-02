package com.tftgogo.domain.patchnote.service;

import com.tftgogo.domain.patchnote.dto.response.PatchChangePageResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;

import java.util.List;

public interface PatchNoteService {

    List<PatchNoteResponse> getPatchNotes();

    PatchChangePageResponse getPatchChanges(
            String version,
            String category,
            String type,
            String impact,
            String query,
            Integer page,
            Integer pageSize
    );
}
