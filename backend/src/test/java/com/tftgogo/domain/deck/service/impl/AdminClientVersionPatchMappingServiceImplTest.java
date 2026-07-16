package com.tftgogo.domain.deck.service.impl;

import com.tftgogo.domain.deck.dto.request.AdminClientVersionPatchMappingRequest;
import com.tftgogo.domain.deck.dto.response.ClientVersionPatchMappingResponse;
import com.tftgogo.domain.deck.entity.ClientVersionPatchMapping;
import com.tftgogo.domain.deck.repository.ClientVersionPatchMappingRepository;
import com.tftgogo.global.config.CacheConfig;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminClientVersionPatchMappingServiceImplTest {

    @Mock
    private ClientVersionPatchMappingRepository clientVersionPatchMappingRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private AdminClientVersionPatchMappingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminClientVersionPatchMappingServiceImpl(clientVersionPatchMappingRepository, cacheManager);
    }

    // meta_decks에는 원본 client version이 저장되고 표시용 패치 번호는 조회 시점에 매핑을 적용해 계산되므로,
    // 매핑 등록/수정/삭제는 더 이상 meta_decks에 소급 반영(bulk UPDATE)할 필요가 없다 — 캐시만 비우면 다음 조회부터 바로 반영된다.

    @Test
    void 매핑을_생성하면_캐시를_비운다() {
        // given
        AdminClientVersionPatchMappingRequest request = requestOf("16.13", "17.6");
        when(clientVersionPatchMappingRepository.findByClientVersion("16.13")).thenReturn(Optional.empty());
        when(clientVersionPatchMappingRepository.save(any())).thenAnswer(invocation -> {
            ClientVersionPatchMapping saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        when(cacheManager.getCache(CacheConfig.META_DECKS)).thenReturn(cache);

        // when
        ClientVersionPatchMappingResponse response = service.createMapping(request);

        // then
        assertThat(response.getClientVersion()).isEqualTo("16.13");
        assertThat(response.getPatchVersion()).isEqualTo("17.6");
        verify(cache).clear();
    }

    @Test
    void 이미_등록된_클라이언트_버전을_생성하면_예외() {
        // given
        AdminClientVersionPatchMappingRequest request = requestOf("16.13", "17.6");
        when(clientVersionPatchMappingRepository.findByClientVersion("16.13"))
                .thenReturn(Optional.of(mappingOf(1L, "16.13", "17.5")));

        // when, then
        assertThatThrownBy(() -> service.createMapping(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CLIENT_VERSION_PATCH_MAPPING_ALREADY_EXISTS));
        verify(clientVersionPatchMappingRepository, never()).save(any());
    }

    @Test
    void 존재하지_않는_매핑을_수정하면_예외() {
        // given
        when(clientVersionPatchMappingRepository.findById(999L)).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> service.updateMapping(999L, requestOf("16.13", "17.6")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CLIENT_VERSION_PATCH_MAPPING_NOT_FOUND));
    }

    @Test
    void 매핑을_수정하면_캐시를_비운다() {
        // given
        ClientVersionPatchMapping mapping = mappingOf(1L, "16.13", "17.5");
        when(clientVersionPatchMappingRepository.findById(1L)).thenReturn(Optional.of(mapping));
        when(clientVersionPatchMappingRepository.findByClientVersion("16.13")).thenReturn(Optional.of(mapping));
        when(cacheManager.getCache(CacheConfig.META_DECKS)).thenReturn(cache);

        // when
        ClientVersionPatchMappingResponse response = service.updateMapping(1L, requestOf("16.13", "17.6"));

        // then: meta_decks는 조회 시점에 매핑을 적용하므로 별도 소급 반영 없이 캐시만 비우면 된다
        assertThat(response.getPatchVersion()).isEqualTo("17.6");
        verify(cache).clear();
    }

    @Test
    void 존재하지_않는_매핑을_삭제하면_예외() {
        // given
        when(clientVersionPatchMappingRepository.findById(999L)).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> service.deleteMapping(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CLIENT_VERSION_PATCH_MAPPING_NOT_FOUND));
    }

    @Test
    void 매핑을_삭제하면_캐시를_비운다() {
        // given: 삭제 즉시 원본 client version이 표시값으로 노출되어야 하므로 캐시를 비워야 한다
        ClientVersionPatchMapping mapping = mappingOf(1L, "16.13", "17.6");
        when(clientVersionPatchMappingRepository.findById(1L)).thenReturn(Optional.of(mapping));
        when(cacheManager.getCache(CacheConfig.META_DECKS)).thenReturn(cache);

        // when
        service.deleteMapping(1L);

        // then
        verify(clientVersionPatchMappingRepository).delete(mapping);
        verify(cache).clear();
    }

    @Test
    void 매핑_목록은_클라이언트_버전_오름차순으로_반환된다() {
        // given
        when(clientVersionPatchMappingRepository.findAllByOrderByClientVersionAsc())
                .thenReturn(List.of(mappingOf(1L, "16.13", "17.6"), mappingOf(2L, "16.14", "17.7")));

        // when
        List<ClientVersionPatchMappingResponse> responses = service.getMappings();

        // then
        assertThat(responses).extracting(ClientVersionPatchMappingResponse::getClientVersion)
                .containsExactly("16.13", "16.14");
    }

    private AdminClientVersionPatchMappingRequest requestOf(String clientVersion, String patchVersion) {
        AdminClientVersionPatchMappingRequest request = new AdminClientVersionPatchMappingRequest();
        ReflectionTestUtils.setField(request, "clientVersion", clientVersion);
        ReflectionTestUtils.setField(request, "patchVersion", patchVersion);
        return request;
    }

    private ClientVersionPatchMapping mappingOf(Long id, String clientVersion, String patchVersion) {
        ClientVersionPatchMapping mapping = ClientVersionPatchMapping.builder()
                .clientVersion(clientVersion)
                .patchVersion(patchVersion)
                .build();
        ReflectionTestUtils.setField(mapping, "id", id);
        return mapping;
    }
}
