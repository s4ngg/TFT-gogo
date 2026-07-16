package com.tftgogo.domain.deck.service;

import com.tftgogo.domain.deck.dto.request.AdminClientVersionPatchMappingRequest;
import com.tftgogo.domain.deck.dto.response.ClientVersionPatchMappingResponse;

import java.util.List;

public interface AdminClientVersionPatchMappingService {

    List<ClientVersionPatchMappingResponse> getMappings();

    ClientVersionPatchMappingResponse createMapping(AdminClientVersionPatchMappingRequest request);

    ClientVersionPatchMappingResponse updateMapping(Long mappingId, AdminClientVersionPatchMappingRequest request);

    void deleteMapping(Long mappingId);
}
