package com.tftgogo.domain.patchnote.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.patchnote.dto.response.PatchChangePageResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
import com.tftgogo.domain.patchnote.entity.PatchChangeCategory;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchChangeImpact;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchNoteServiceImplTest {

    @Mock
    private PatchNoteRepository patchNoteRepository;

    @Mock
    private PatchChangeRepository patchChangeRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PatchNoteServiceImpl patchNoteService;

    @Test
    void 패치노트_목록은_highlights와_changeCount를_응답한다() {
        // given
        PatchNote currentPatch = patchNote("17.0", true);
        when(patchNoteRepository.findByActiveTrueAndDeletedAtIsNullOrderByCurrentDescPublishedAtDescIdDesc())
                .thenReturn(List.of(currentPatch));
        when(patchChangeRepository.countByPatchNotes(List.of(currentPatch)))
                .thenReturn(List.of(patchChangeCount(currentPatch.getId(), 3L)));

        // when
        List<PatchNoteResponse> response = patchNoteService.getPatchNotes();

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getHighlights()).containsExactly("챔피언 밸런스 조정", "시너지 조정");
        assertThat(response.get(0).getChangeCount()).isEqualTo(3L);
    }

    @Test
    void 존재하지_않는_패치버전은_예외를_던진다() {
        // given
        when(patchNoteRepository.findByVersionAndActiveTrueAndDeletedAtIsNull("17.9"))
                .thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> patchNoteService.getPatchChanges(
                "17.9",
                null,
                null,
                null,
                null,
                1,
                10
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PATCH_NOTE_NOT_FOUND));
    }

    @Test
    void 변경사항_조회는_필터와_stats를_분리해서_응답한다() {
        // given
        PatchNote patchNote = patchNote("17.0", true);
        PatchChange buff = patchChange(patchNote, PatchChangeCategory.CHAMPION, PatchChangeType.BUFF, PatchChangeImpact.HIGH, "카이사", 1);
        PatchChange nerf = patchChange(patchNote, PatchChangeCategory.ITEM, PatchChangeType.NERF, PatchChangeImpact.LOW, "죽음검", 2);
        when(patchNoteRepository.findByVersionAndActiveTrueAndDeletedAtIsNull("17.0"))
                .thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.findByPatchNoteAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc(patchNote))
                .thenReturn(List.of(buff, nerf));
        when(patchChangeRepository.findFilteredChanges(
                patchNote,
                PatchChangeCategory.CHAMPION,
                PatchChangeType.BUFF,
                PatchChangeImpact.HIGH,
                "카이사"
        )).thenReturn(List.of(buff));

        // when
        PatchChangePageResponse response = patchNoteService.getPatchChanges(
                "17.0",
                "CHAMPION",
                "BUFF",
                "HIGH",
                "카이사",
                1,
                10
        );

        // then
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getStats().getTotalChanges()).isEqualTo(2L);
        assertThat(response.getStats().getCategoryCounts()).containsEntry("ALL", 2L);
        assertThat(response.getStats().getTypeCounts()).containsEntry("BUFF", 1L);
        assertThat(response.getStats().getHighImpactCount()).isEqualTo(1L);
        verify(patchChangeRepository).findFilteredChanges(
                patchNote,
                PatchChangeCategory.CHAMPION,
                PatchChangeType.BUFF,
                PatchChangeImpact.HIGH,
                "카이사"
        );
    }

    @Test
    void 검색어는_like_와일드카드를_이스케이프해서_조회한다() {
        // given
        PatchNote patchNote = patchNote("17.0", true);
        when(patchNoteRepository.findByVersionAndActiveTrueAndDeletedAtIsNull("17.0"))
                .thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.findByPatchNoteAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc(patchNote))
                .thenReturn(List.of());
        when(patchChangeRepository.findFilteredChanges(
                patchNote,
                null,
                null,
                null,
                "카\\%\\_\\\\이사"
        )).thenReturn(List.of());

        // when
        PatchChangePageResponse response = patchNoteService.getPatchChanges(
                "17.0",
                null,
                null,
                null,
                "카%_\\이사",
                1,
                10
        );

        // then
        assertThat(response.getItems()).isEmpty();
        verify(patchChangeRepository).findFilteredChanges(
                patchNote,
                null,
                null,
                null,
                "카\\%\\_\\\\이사"
        );
    }

    @Test
    void 변경사항_조회는_두번째_페이지를_페이지크기만큼_잘라서_응답한다() {
        // given
        PatchNote patchNote = patchNote("17.0", true);
        PatchChange buff = patchChange(patchNote, PatchChangeCategory.CHAMPION, PatchChangeType.BUFF, PatchChangeImpact.HIGH, "카이사", 1);
        PatchChange nerf = patchChange(patchNote, PatchChangeCategory.ITEM, PatchChangeType.NERF, PatchChangeImpact.LOW, "죽음검", 2);
        when(patchNoteRepository.findByVersionAndActiveTrueAndDeletedAtIsNull("17.0"))
                .thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.findByPatchNoteAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc(patchNote))
                .thenReturn(List.of(buff, nerf));
        when(patchChangeRepository.findFilteredChanges(
                patchNote,
                null,
                null,
                null,
                null
        )).thenReturn(List.of(buff, nerf));

        // when
        PatchChangePageResponse response = patchNoteService.getPatchChanges(
                "17.0",
                null,
                null,
                null,
                null,
                2,
                1
        );

        // then
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getTargetName()).isEqualTo("죽음검");
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getPageSize()).isEqualTo(1);
        assertThat(response.getTotalItems()).isEqualTo(2L);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getStats().getTotalChanges()).isEqualTo(2L);
    }

    @Test
    void 잘못된_page와_pageSize는_예외를_던진다() {
        // given
        PatchNote patchNote = patchNote("17.0", true);
        when(patchNoteRepository.findByVersionAndActiveTrueAndDeletedAtIsNull("17.0"))
                .thenReturn(Optional.of(patchNote));

        // when, then
        assertThatThrownBy(() -> patchNoteService.getPatchChanges(
                "17.0",
                null,
                null,
                null,
                null,
                0,
                10
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertThatThrownBy(() -> patchNoteService.getPatchChanges(
                "17.0",
                null,
                null,
                null,
                null,
                1,
                0
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 필터_결과가_없으면_빈_items와_정상_페이지_메타를_응답한다() {
        // given
        PatchNote patchNote = patchNote("17.0", true);
        PatchChange buff = patchChange(patchNote, PatchChangeCategory.CHAMPION, PatchChangeType.BUFF, PatchChangeImpact.HIGH, "카이사", 1);
        when(patchNoteRepository.findByVersionAndActiveTrueAndDeletedAtIsNull("17.0"))
                .thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.findByPatchNoteAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc(patchNote))
                .thenReturn(List.of(buff));
        when(patchChangeRepository.findFilteredChanges(
                patchNote,
                PatchChangeCategory.ITEM,
                null,
                null,
                null
        )).thenReturn(List.of());

        // when
        PatchChangePageResponse response = patchNoteService.getPatchChanges(
                "17.0",
                "ITEM",
                null,
                null,
                null,
                1,
                10
        );

        // then
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalItems()).isZero();
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getStats().getTotalChanges()).isEqualTo(1L);
    }

    @Test
    void 잘못된_category는_예외를_던진다() {
        // given
        PatchNote patchNote = patchNote("17.0", true);
        when(patchNoteRepository.findByVersionAndActiveTrueAndDeletedAtIsNull("17.0"))
                .thenReturn(Optional.of(patchNote));

        // when, then
        assertThatThrownBy(() -> patchNoteService.getPatchChanges(
                "17.0",
                "UNKNOWN",
                null,
                null,
                null,
                1,
                10
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private PatchNote patchNote(String version, boolean current) {
        PatchNote patchNote = PatchNote.builder()
                .version(version)
                .title(version + " 패치")
                .summary("패치 요약")
                .description("패치 설명")
                .focus("밸런스")
                .imageUrl("https://example.com/patch.png")
                .publishedAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                .current(current)
                .highlightsJson("[\"챔피언 밸런스 조정\",\"시너지 조정\"]")
                .active(true)
                .build();
        ReflectionTestUtils.setField(patchNote, "id", 1L);
        return patchNote;
    }

    private PatchChange patchChange(
            PatchNote patchNote,
            PatchChangeCategory category,
            PatchChangeType type,
            PatchChangeImpact impact,
            String targetName,
            int sortOrder
    ) {
        return PatchChange.builder()
                .patchNote(patchNote)
                .category(category)
                .changeType(type)
                .impact(impact)
                .targetKey(targetName.toLowerCase())
                .targetName(targetName)
                .summary(targetName + " 변경")
                .beforeValue("이전")
                .afterValue("이후")
                .imageUrl("https://example.com/" + targetName + ".png")
                .tagsJson("[\"테스트\"]")
                .sortOrder(sortOrder)
                .active(true)
                .build();
    }

    private PatchChangeRepository.PatchChangeCount patchChangeCount(Long patchNoteId, Long changeCount) {
        return new PatchChangeRepository.PatchChangeCount() {
            @Override
            public Long getPatchNoteId() {
                return patchNoteId;
            }

            @Override
            public Long getChangeCount() {
                return changeCount;
            }
        };
    }
}
