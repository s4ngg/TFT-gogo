package com.tftgogo.domain.deck.service.impl;

import com.tftgogo.domain.deck.dto.request.AdminClientVersionPatchMappingRequest;
import com.tftgogo.domain.deck.dto.response.ClientVersionPatchMappingResponse;
import com.tftgogo.domain.deck.entity.ClientVersionPatchMapping;
import com.tftgogo.domain.deck.repository.ClientVersionPatchMappingRepository;
import com.tftgogo.domain.deck.service.AdminClientVersionPatchMappingService;
import com.tftgogo.global.config.CacheConfig;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminClientVersionPatchMappingServiceImpl implements AdminClientVersionPatchMappingService {

    private final ClientVersionPatchMappingRepository clientVersionPatchMappingRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    public List<ClientVersionPatchMappingResponse> getMappings() {
        return clientVersionPatchMappingRepository.findAllByOrderByClientVersionAsc().stream()
                .map(ClientVersionPatchMappingResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public ClientVersionPatchMappingResponse createMapping(AdminClientVersionPatchMappingRequest request) {
        String clientVersion = request.getClientVersion().trim();
        String patchVersion = request.getPatchVersion().trim();
        validateUniqueClientVersion(clientVersion, null);

        ClientVersionPatchMapping mapping = ClientVersionPatchMapping.builder()
                .clientVersion(clientVersion)
                .patchVersion(patchVersion)
                .build();
        ClientVersionPatchMapping savedMapping = clientVersionPatchMappingRepository.save(mapping);

        evictMetaDecksCache();
        return ClientVersionPatchMappingResponse.from(savedMapping);
    }

    @Override
    @Transactional
    public ClientVersionPatchMappingResponse updateMapping(Long mappingId, AdminClientVersionPatchMappingRequest request) {
        ClientVersionPatchMapping mapping = findMapping(mappingId);
        String clientVersion = request.getClientVersion().trim();
        String patchVersion = request.getPatchVersion().trim();
        validateUniqueClientVersion(clientVersion, mappingId);

        mapping.update(clientVersion, patchVersion);

        evictMetaDecksCache();
        return ClientVersionPatchMappingResponse.from(mapping);
    }

    @Override
    @Transactional
    public void deleteMapping(Long mappingId) {
        ClientVersionPatchMapping mapping = findMapping(mappingId);
        clientVersionPatchMappingRepository.delete(mapping);

        evictMetaDecksCache();
    }

    private ClientVersionPatchMapping findMapping(Long mappingId) {
        return clientVersionPatchMappingRepository.findById(mappingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_VERSION_PATCH_MAPPING_NOT_FOUND));
    }

    private void validateUniqueClientVersion(String clientVersion, Long excludedMappingId) {
        Optional<ClientVersionPatchMapping> existing = clientVersionPatchMappingRepository.findByClientVersion(clientVersion);
        if (existing.isPresent() && !existing.get().getId().equals(excludedMappingId)) {
            throw new BusinessException(ErrorCode.CLIENT_VERSION_PATCH_MAPPING_ALREADY_EXISTS);
        }
    }

    // meta_decks에는 원본 client version이 저장되고 표시용 패치 번호는 조회 시점에 매핑을 적용해 계산되므로,
    // 매핑을 바꾸면 다음 조회부터 바로 반영된다. 캐시된 응답만 무효화하면 되고 별도 소급 반영(bulk UPDATE)은 필요 없다.
    private void evictMetaDecksCache() {
        Optional.ofNullable(cacheManager.getCache(CacheConfig.META_DECKS))
                .ifPresent(Cache::clear);
    }
}
