package com.tftgogo.domain.deck.service.impl;

import com.tftgogo.domain.deck.dto.request.AdminClientVersionPatchMappingRequest;
import com.tftgogo.domain.deck.dto.response.ClientVersionPatchMappingResponse;
import com.tftgogo.domain.deck.entity.ClientVersionPatchMapping;
import com.tftgogo.domain.deck.repository.ClientVersionPatchMappingRepository;
import com.tftgogo.domain.deck.repository.MetaDeckRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminClientVersionPatchMappingServiceImplTest {

    @Mock
    private ClientVersionPatchMappingRepository clientVersionPatchMappingRepository;

    @Mock
    private MetaDeckRepository metaDeckRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private AdminClientVersionPatchMappingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminClientVersionPatchMappingServiceImpl(
                clientVersionPatchMappingRepository, metaDeckRepository, cacheManager);
    }

    @Test
    void 매핑을_생성하면_기존_메타덱_데이터에_소급_반영하고_캐시를_비운다() {
        // given
        AdminClientVersionPatchMappingRequest request = requestOf("16.13", "17.6");
        when(clientVersionPatchMappingRepository.findByClientVersion("16.13")).thenReturn(Optional.empty());
        when(clientVersionPatchMappingRepository.save(any())).thenAnswer(invocation -> {
            ClientVersionPatchMapping saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        when(metaDeckRepository.updatePatchVersionByClientVersion("16.13", "17.6")).thenReturn(3);
        when(cacheManager.getCache("metaDecks")).thenReturn(cache);

        // when
        ClientVersionPatchMappingResponse response = service.createMapping(request);

        // then
        assertThat(response.getClientVersion()).isEqualTo("16.13");
        assertThat(response.getPatchVersion()).isEqualTo("17.6");
        verify(metaDeckRepository).updatePatchVersionByClientVersion("16.13", "17.6");
        verify(cache).clear();
    }

    @Test
    void 소급_반영된_기존_데이터가_없으면_캐시를_비우지_않는다() {
        // given
        AdminClientVersionPatchMappingRequest request = requestOf("16.13", "17.6");
        when(clientVersionPatchMappingRepository.findByClientVersion("16.13")).thenReturn(Optional.empty());
        when(clientVersionPatchMappingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(metaDeckRepository.updatePatchVersionByClientVersion("16.13", "17.6")).thenReturn(0);

        // when
        service.createMapping(request);

        // then
        verify(cacheManager, never()).getCache(anyString());
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
    void 매핑을_수정하면_원래_클라이언트_버전_기준으로_기존_데이터를_소급_반영한다() {
        // given
        ClientVersionPatchMapping mapping = mappingOf(1L, "16.13", "17.5");
        when(clientVersionPatchMappingRepository.findById(1L)).thenReturn(Optional.of(mapping));
        when(clientVersionPatchMappingRepository.findByClientVersion("16.13")).thenReturn(Optional.of(mapping));
        when(metaDeckRepository.updatePatchVersionByClientVersion("16.13", "17.6")).thenReturn(2);
        when(cacheManager.getCache("metaDecks")).thenReturn(cache);

        // when
        ClientVersionPatchMappingResponse response = service.updateMapping(1L, requestOf("16.13", "17.6"));

        // then
        assertThat(response.getPatchVersion()).isEqualTo("17.6");
        verify(metaDeckRepository).updatePatchVersionByClientVersion("16.13", "17.6");
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
