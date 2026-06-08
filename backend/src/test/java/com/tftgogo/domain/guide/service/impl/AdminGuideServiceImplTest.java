package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.request.AdminGuideRequest;
import com.tftgogo.domain.guide.dto.response.AdminGuideResponse;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.GuideRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminGuideServiceImplTest {

    @Mock
    private GuideRepository guideRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AdminGuideServiceImpl adminGuideService;

    @Test
    void 관리자_목록은_필터로_조회하고_dataJson을_object로_응답한다() throws Exception {
        // given
        Guide guide = guide(1L, GuideType.CHAMPION, "tft17_jinx", "징크스", "17.3", true);
        when(guideRepository.findAdminGuides(GuideType.CHAMPION, "17.3", true))
                .thenReturn(List.of(guide));

        // when
        List<AdminGuideResponse> responses = adminGuideService.getAdminGuides(GuideType.CHAMPION, " 17.3 ", true);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getDataJson().get("cost").asInt()).isEqualTo(4);
        verify(guideRepository).findAdminGuides(GuideType.CHAMPION, "17.3", true);
    }

    @Test
    void 게임가이드를_생성할_때_dataJson을_문자열로_저장한다() throws Exception {
        // given
        AdminGuideRequest request = request(
                GuideType.CHAMPION,
                " tft17_jinx ",
                " 징크스 ",
                "17.3",
                dataJson("{\"cost\":4,\"role\":\"carry\"}"),
                true
        );
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                GuideType.CHAMPION,
                "tft17_jinx",
                "17.3"
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        AdminGuideResponse response = adminGuideService.createGuide(request);

        // then
        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository).saveAndFlush(guideCaptor.capture());
        Guide savedGuide = guideCaptor.getValue();
        assertThat(savedGuide.getTargetKey()).isEqualTo("tft17_jinx");
        assertThat(savedGuide.getName()).isEqualTo("징크스");
        assertThat(savedGuide.getDataJson()).contains("\"cost\":4");
        assertThat(response.getDataJson().get("role").asText()).isEqualTo("carry");
    }

    @Test
    void 같은_타입과_대상키와_패치버전이_있으면_생성할_수_없다() throws Exception {
        // given
        AdminGuideRequest request = request(
                GuideType.CHAMPION,
                "tft17_jinx",
                "징크스",
                "17.3",
                dataJson("{\"cost\":4}"),
                true
        );
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                GuideType.CHAMPION,
                "tft17_jinx",
                "17.3"
        )).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> adminGuideService.createGuide(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GUIDE_ALREADY_EXISTS));
    }

    @Test
    void dataJson이_object가_아니면_생성할_수_없다() throws Exception {
        // given
        AdminGuideRequest request = request(
                GuideType.CHAMPION,
                "tft17_jinx",
                "징크스",
                "17.3",
                dataJson("[\"invalid\"]"),
                true
        );
        // when, then
        assertThatThrownBy(() -> adminGuideService.createGuide(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 생성_중_DB_중복_제약이_발생하면_GUIDE_ALREADY_EXISTS로_변환한다() throws Exception {
        // given
        AdminGuideRequest request = request(
                GuideType.CHAMPION,
                "tft17_jinx",
                "징크스",
                "17.3",
                dataJson("{\"cost\":4}"),
                true
        );
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                GuideType.CHAMPION,
                "tft17_jinx",
                "17.3"
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate guide"));

        // when, then
        assertThatThrownBy(() -> adminGuideService.createGuide(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GUIDE_ALREADY_EXISTS));
    }

    @Test
    void 게임가이드를_수정하면_기존_엔티티를_갱신한다() throws Exception {
        // given
        Guide guide = guide(1L, GuideType.CHAMPION, "tft17_jinx", "징크스", "17.3", true);
        AdminGuideRequest request = request(
                GuideType.CHAMPION,
                "tft17_kaisa",
                "카이사",
                "17.3",
                dataJson("{\"cost\":4,\"role\":\"carry\"}"),
                false
        );
        when(guideRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(guide));
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersionAndIdNot(
                GuideType.CHAMPION,
                "tft17_kaisa",
                "17.3",
                1L
        )).thenReturn(false);

        // when
        AdminGuideResponse response = adminGuideService.updateGuide(1L, request);

        // then
        assertThat(guide.getTargetKey()).isEqualTo("tft17_kaisa");
        assertThat(guide.getName()).isEqualTo("카이사");
        assertThat(guide.isActive()).isFalse();
        assertThat(response.getDataJson().get("role").asText()).isEqualTo("carry");
        verify(guideRepository).flush();
    }

    @Test
    void 수정_중_DB_중복_제약이_발생하면_GUIDE_ALREADY_EXISTS로_변환한다() throws Exception {
        // given
        Guide guide = guide(1L, GuideType.CHAMPION, "tft17_jinx", "징크스", "17.3", true);
        AdminGuideRequest request = request(
                GuideType.CHAMPION,
                "tft17_kaisa",
                "카이사",
                "17.3",
                dataJson("{\"cost\":4,\"role\":\"carry\"}"),
                false
        );
        when(guideRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(guide));
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersionAndIdNot(
                GuideType.CHAMPION,
                "tft17_kaisa",
                "17.3",
                1L
        )).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate guide"))
                .when(guideRepository)
                .flush();

        // when, then
        assertThatThrownBy(() -> adminGuideService.updateGuide(1L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GUIDE_ALREADY_EXISTS));
    }

    @Test
    void 존재하지_않는_게임가이드는_수정할_수_없다() throws Exception {
        // given
        AdminGuideRequest request = request(
                GuideType.CHAMPION,
                "tft17_jinx",
                "징크스",
                "17.3",
                dataJson("{\"cost\":4}"),
                true
        );
        when(guideRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> adminGuideService.updateGuide(1L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GUIDE_NOT_FOUND));
    }

    @Test
    void 저장된_dataJson이_null이면_GUIDE_INVALID_DATA로_변환한다() {
        // given
        Guide guide = guide(1L, GuideType.CHAMPION, "tft17_jinx", "징크스", "17.3", true);
        ReflectionTestUtils.setField(guide, "dataJson", null);
        when(guideRepository.findAdminGuides(null, null, null)).thenReturn(List.of(guide));

        // when, then
        assertThatThrownBy(() -> adminGuideService.getAdminGuides(null, null, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GUIDE_INVALID_DATA));
    }

    @Test
    void 게임가이드를_soft_delete한다() {
        // given
        Guide guide = guide(1L, GuideType.CHAMPION, "tft17_jinx", "징크스", "17.3", true);
        when(guideRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(guide));

        // when
        adminGuideService.deleteGuide(1L);

        // then
        assertThat(guide.isActive()).isFalse();
        assertThat(guide.getDeletedAt()).isNotNull();
    }

    private AdminGuideRequest request(
            GuideType guideType,
            String targetKey,
            String name,
            String patchVersion,
            JsonNode dataJson,
            Boolean active
    ) {
        AdminGuideRequest request = new AdminGuideRequest();
        ReflectionTestUtils.setField(request, "guideType", guideType);
        ReflectionTestUtils.setField(request, "targetKey", targetKey);
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "summary", "핵심 운용 요약");
        ReflectionTestUtils.setField(request, "imageUrl", "https://example.com/" + targetKey.trim() + ".png");
        ReflectionTestUtils.setField(request, "dataJson", dataJson);
        ReflectionTestUtils.setField(request, "patchVersion", patchVersion);
        ReflectionTestUtils.setField(request, "sortOrder", 1);
        ReflectionTestUtils.setField(request, "active", active);
        return request;
    }

    private Guide guide(Long id, GuideType guideType, String targetKey, String name, String patchVersion, boolean active) {
        Guide guide = Guide.builder()
                .guideType(guideType)
                .targetKey(targetKey)
                .name(name)
                .summary("핵심 운용 요약")
                .imageUrl("https://example.com/" + targetKey + ".png")
                .dataJson("{\"cost\":4,\"role\":\"carry\"}")
                .patchVersion(patchVersion)
                .sortOrder(1)
                .active(active)
                .build();
        ReflectionTestUtils.setField(guide, "id", id);
        return guide;
    }

    private JsonNode dataJson(String json) throws Exception {
        return objectMapper.readTree(json);
    }
}
