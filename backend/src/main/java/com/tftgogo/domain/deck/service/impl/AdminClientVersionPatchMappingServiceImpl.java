package com.tftgogo.domain.deck.service.impl;

import com.tftgogo.domain.deck.dto.request.AdminClientVersionPatchMappingRequest;
import com.tftgogo.domain.deck.dto.response.ClientVersionPatchMappingResponse;
import com.tftgogo.domain.deck.entity.ClientVersionPatchMapping;
import com.tftgogo.domain.deck.repository.ClientVersionPatchMappingRepository;
import com.tftgogo.domain.deck.repository.MetaDeckRepository;
import com.tftgogo.domain.deck.service.AdminClientVersionPatchMappingService;
import com.tftgogo.global.config.CacheConfig;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminClientVersionPatchMappingServiceImpl implements AdminClientVersionPatchMappingService {

    private static final Logger logger = LogManager.getLogger(AdminClientVersionPatchMappingServiceImpl.class);

    private final ClientVersionPatchMappingRepository clientVersionPatchMappingRepository;
    private final MetaDeckRepository metaDeckRepository;
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

        backfillExistingMetaDecks(clientVersion, patchVersion);
        return ClientVersionPatchMappingResponse.from(savedMapping);
    }

    @Override
    @Transactional
    public ClientVersionPatchMappingResponse updateMapping(Long mappingId, AdminClientVersionPatchMappingRequest request) {
        ClientVersionPatchMapping mapping = findMapping(mappingId);
        String oldClientVersion = mapping.getClientVersion();
        String oldPatchVersion = mapping.getPatchVersion();
        String clientVersion = request.getClientVersion().trim();
        String patchVersion = request.getPatchVersion().trim();
        validateUniqueClientVersion(clientVersion, mappingId);

        mapping.update(clientVersion, patchVersion);

        // 기존 원본 클라이언트 버전(아직 매핑 미적용) 행과, 이전에 이미 매핑 적용되어
        // oldPatchVersion으로 저장된 행을 모두 새 patchVersion으로 소급 반영한다.
        backfillExistingMetaDecks(oldClientVersion, patchVersion);
        if (!oldPatchVersion.equals(oldClientVersion)) {
            backfillExistingMetaDecks(oldPatchVersion, patchVersion);
        }
        return ClientVersionPatchMappingResponse.from(mapping);
    }

    @Override
    @Transactional
    public void deleteMapping(Long mappingId) {
        ClientVersionPatchMapping mapping = findMapping(mappingId);
        clientVersionPatchMappingRepository.delete(mapping);
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

    // meta_decks.patchVersion에 저장된 이전 값(원본 클라이언트 버전 또는 과거에 매핑 적용된 패치 번호)을
    // 새 patchVersion으로 소급 반영
    private void backfillExistingMetaDecks(String previousPatchVersionValue, String patchVersion) {
        int updatedRowCount = metaDeckRepository.updatePatchVersionByPreviousValue(previousPatchVersionValue, patchVersion);
        if (updatedRowCount > 0) {
            logger.info(
                    "클라이언트 버전 매핑 적용으로 기존 메타덱 패치 번호 소급 반영 - previousValue={}, patchVersion={}, updatedRowCount={}",
                    previousPatchVersionValue, patchVersion, updatedRowCount
            );
            Optional.ofNullable(cacheManager.getCache(CacheConfig.META_DECKS))
                    .ifPresent(Cache::clear);
        }
    }
}
