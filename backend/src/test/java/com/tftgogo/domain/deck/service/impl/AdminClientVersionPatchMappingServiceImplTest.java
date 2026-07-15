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
        when(metaDeckRepository.updatePatchVersionByPreviousValue("16.13", "17.6")).thenReturn(3);
        when(cacheManager.getCache("metaDecks")).thenReturn(cache);

        // when
        ClientVersionPatchMappingResponse response = service.createMapping(request);

        // then
        assertThat(response.getClientVersion()).isEqualTo("16.13");
        assertThat(response.getPatchVersion()).isEqualTo("17.6");
        verify(metaDeckRepository).updatePatchVersionByPreviousValue("16.13", "17.6");
        verify(cache).clear();
    }

    @Test
    void 소급_반영된_기존_데이터가_없으면_캐시를_비우지_않는다() {
        // given
        AdminClientVersionPatchMappingRequest request = requestOf("16.13", "17.6");
        when(clientVersionPatchMappingRepository.findByClientVersion("16.13")).thenReturn(Optional.empty());
        when(clientVersionPatchMappingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(metaDeckRepository.updatePatchVersionByPreviousValue("16.13", "17.6")).thenReturn(0);

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
    void 매핑을_수정하면_원본_클라이언트_버전과_이전_패치_번호_둘_다_기준으로_소급_반영한다() {
        // given: 이전에 patchVersion이 17.5로 한 번 수정된 적이 있는 매핑을 다시 17.6으로 수정
        ClientVersionPatchMapping mapping = mappingOf(1L, "16.13", "17.5");
        when(clientVersionPatchMappingRepository.findById(1L)).thenReturn(Optional.of(mapping));
        when(clientVersionPatchMappingRepository.findByClientVersion("16.13")).thenReturn(Optional.of(mapping));
        when(metaDeckRepository.updatePatchVersionByPreviousValue("16.13", "17.6")).thenReturn(0);
        when(metaDeckRepository.updatePatchVersionByPreviousValue("17.5", "17.6")).thenReturn(2);
        when(cacheManager.getCache("metaDecks")).thenReturn(cache);

        // when
        ClientVersionPatchMappingResponse response = service.updateMapping(1L, requestOf("16.13", "17.6"));

        // then
        assertThat(response.getPatchVersion()).isEqualTo("17.6");
        // 아직 매핑이 한 번도 적용되지 않은 원본 클라이언트 버전(16.13) 행도 조회
        verify(metaDeckRepository).updatePatchVersionByPreviousValue("16.13", "17.6");
        // 이전 매핑(17.5)으로 이미 소급 반영됐던 행도 새 값으로 다시 반영
        verify(metaDeckRepository).updatePatchVersionByPreviousValue("17.5", "17.6");
        verify(cache).clear();
    }

    @Test
    void 클라이언트_버전과_패치_번호가_같았던_매핑을_수정하면_소급_반영을_중복_실행하지_않는다() {
        // given: 등록 직후라 clientVersion과 patchVersion이 아직 갈라지지 않은 상태에서 수정
        ClientVersionPatchMapping mapping = mappingOf(1L, "16.13", "16.13");
        when(clientVersionPatchMappingRepository.findById(1L)).thenReturn(Optional.of(mapping));
        when(clientVersionPatchMappingRepository.findByClientVersion("16.13")).thenReturn(Optional.of(mapping));
        when(metaDeckRepository.updatePatchVersionByPreviousValue("16.13", "17.6")).thenReturn(1);

        // when
        service.updateMapping(1L, requestOf("16.13", "17.6"));

        // then
        verify(metaDeckRepository, org.mockito.Mockito.times(1))
                .updatePatchVersionByPreviousValue(anyString(), anyString());
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
