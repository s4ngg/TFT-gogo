package com.tftgogo.domain.patchnote.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.patchnote.dto.response.PatchChangePageResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeCategory;
import com.tftgogo.domain.patchnote.entity.PatchChangeImpact;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository.CategoryChangeCount;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository.PatchChangeStatsCount;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository.TypeChangeCount;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
        LocalDateTime expectedCutoffStart = LocalDateTime.now().minusMonths(6).minusMinutes(1);
        when(patchNoteRepository.findPublicHistorySinceIncludingCurrent(any(LocalDateTime.class)))
                .thenReturn(List.of(currentPatch));
        when(patchChangeRepository.countByPatchNotes(List.of(currentPatch)))
                .thenReturn(List.of(patchChangeCount(currentPatch.getId(), 3L)));

        // when
        List<PatchNoteResponse> response = patchNoteService.getPatchNotes();

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getHighlights()).containsExactly("챔피언 밸런스 조정", "시너지 조정");
        assertThat(response.get(0).getChangeCount()).isEqualTo(3L);
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(patchNoteRepository).findPublicHistorySinceIncludingCurrent(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue())
                .isBetween(expectedCutoffStart, LocalDateTime.now().minusMonths(6).plusMinutes(1));
    }

    @Test
    void 존재하지_않는_패치버전은_예외를_던진다() {
        // given
        when(patchNoteRepository.findByVersionAndDeletedAtIsNull("17.9"))
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
    void 변경사항_조회는_검색_조건을_반영한_stats를_응답한다() {
        // given
        PatchNote patchNote = patchNote("17.0", true);
        PatchChange buff = patchChange(patchNote, PatchChangeCategory.CHAMPION, PatchChangeType.BUFF, PatchChangeImpact.HIGH, "카이사", 1);
        when(patchNoteRepository.findByVersionAndDeletedAtIsNull("17.0"))
                .thenReturn(Optional.of(patchNote));
        givenPatchChangeStats(
                patchNote,
                PatchChangeType.BUFF,
                PatchChangeImpact.HIGH,
                "카이사",
                List.of(categoryChangeCount(PatchChangeCategory.CHAMPION, 1L)),
                List.of(typeChangeCount(PatchChangeType.BUFF, 1L)),
                1L
        );
        when(patchChangeRepository.findFilteredChanges(
                patchNote,
                PatchChangeCategory.CHAMPION,
                PatchChangeType.BUFF,
                PatchChangeImpact.HIGH,
                "카이사",
                PageRequest.of(0, 10)
        )).thenReturn(patchChangePage(List.of(buff), 0, 10, 1));

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
        assertThat(response.getStats().getTotalChanges()).isEqualTo(1L);
        assertThat(response.getStats().getCategoryCounts()).containsEntry("ALL", 1L);
        assertThat(response.getStats().getCategoryCounts()).containsEntry("CHAMPION", 1L);
        assertThat(response.getStats().getCategoryCounts()).containsEntry("ITEM", 0L);
        assertThat(response.getStats().getTypeCounts()).containsEntry("BUFF", 1L);
        assertThat(response.getStats().getHighImpactCount()).isEqualTo(1L);
        verify(patchChangeRepository).countFilteredChangeStats(
                patchNote,
                PatchChangeType.BUFF,
                PatchChangeImpact.HIGH,
                "카이사"
        );
        verify(patchChangeRepository, never()).countFilteredChangesGroupByCategory(any(), any(), any(), any());
        verify(patchChangeRepository, never()).countFilteredChangesGroupByChangeType(any(), any(), any(), any());
        verify(patchChangeRepository, never()).countFilteredChanges(any(), any(), any(), any(), any());
        verify(patchChangeRepository).findFilteredChanges(
                patchNote,
                PatchChangeCategory.CHAMPION,
                PatchChangeType.BUFF,
                PatchChangeImpact.HIGH,
                "카이사",
                PageRequest.of(0, 10)
        );
        verify(patchChangeRepository, never()).findByPatchNoteOrderBySortOrderAscIdAsc(patchNote);
    }

    @Test
    void low_impact_필터는_highImpactCount를_0으로_응답한다() {
        // given
        PatchNote patchNote = patchNote("17.0", true);
        PatchChange nerf = patchChange(patchNote, PatchChangeCategory.ITEM, PatchChangeType.NERF, PatchChangeImpact.LOW, "징크스", 1);
        when(patchNoteRepository.findByVersionAndDeletedAtIsNull("17.0"))
                .thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.countFilteredChangeStats(
                patchNote,
                PatchChangeType.NERF,
                PatchChangeImpact.LOW,
                "징크스"
        )).thenReturn(List.of(patchChangeStatsCount(
                PatchChangeCategory.ITEM,
                PatchChangeType.NERF,
                PatchChangeImpact.LOW,
                1L
        )));
        when(patchChangeRepository.findFilteredChanges(
                patchNote,
                PatchChangeCategory.ITEM,
                PatchChangeType.NERF,
                PatchChangeImpact.LOW,
                "징크스",
                PageRequest.of(0, 10)
        )).thenReturn(patchChangePage(List.of(nerf), 0, 10, 1));

        // when
        PatchChangePageResponse response = patchNoteService.getPatchChanges(
                "17.0",
                "ITEM",
                "NERF",
                "LOW",
                "징크스",
                1,
                10
        );

        // then
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getStats().getTotalChanges()).isEqualTo(1L);
        assertThat(response.getStats().getHighImpactCount()).isZero();
        verify(patchChangeRepository, never()).countFilteredChanges(
                patchNote,
                null,
                PatchChangeType.NERF,
                PatchChangeImpact.HIGH,
                "징크스"
        );
    }

    @Test
    void 검색어는_like_와일드카드를_이스케이프해서_조회한다() {
        // given
        PatchNote patchNote = patchNote("17.0", true);
        when(patchNoteRepository.findByVersionAndDeletedAtIsNull("17.0"))
                .thenReturn(Optional.of(patchNote));
        givenPatchChangeStats(
                patchNote,
                null,
                null,
                "카\\%\\_\\\\이사",
                List.of(),
                List.of(),
                0L
        );
        when(patchChangeRepository.findFilteredChanges(
                patchNote,
                null,
                null,
                null,
                "카\\%\\_\\\\이사",
                PageRequest.of(0, 10)
        )).thenReturn(patchChangePage(List.of(), 0, 10, 0));

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
        verify(patchChangeRepository).countFilteredChangeStats(
                patchNote,
                null,
                null,
                "카\\%\\_\\\\이사"
        );
        verify(patchChangeRepository).findFilteredChanges(
                patchNote,
                null,
                null,
                null,
                "카\\%\\_\\\\이사",
                PageRequest.of(0, 10)
        );
    }

    @Test
    void 변경사항_조회는_요청한_페이지를_DB_page로_조회한다() {
        // given
        PatchNote patchNote = patchNote("17.0", true);
        PatchChange nerf = patchChange(patchNote, PatchChangeCategory.ITEM, PatchChangeType.NERF, PatchChangeImpact.LOW, "죽음검", 2);
        when(patchNoteRepository.findByVersionAndDeletedAtIsNull("17.0"))
                .thenReturn(Optional.of(patchNote));
        givenPatchChangeStats(
                patchNote,
                List.of(
                        categoryChangeCount(PatchChangeCategory.CHAMPION, 1L),
                        categoryChangeCount(PatchChangeCategory.ITEM, 1L)
                ),
                List.of(
                        typeChangeCount(PatchChangeType.BUFF, 1L),
                        typeChangeCount(PatchChangeType.NERF, 1L)
                ),
                1L
        );
        when(patchChangeRepository.findFilteredChanges(
                patchNote,
                null,
                null,
                null,
                null,
                PageRequest.of(1, 1)
        )).thenReturn(patchChangePage(List.of(nerf), 1, 1, 2));

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
        when(patchNoteRepository.findByVersionAndDeletedAtIsNull("17.0"))
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

        assertThatThrownBy(() -> patchNoteService.getPatchChanges(
                "17.0",
                null,
                null,
                null,
                null,
                1,
                1001
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void pageSize_1000은_한_패치_전체_조회용으로_허용한다() {
        // given
        PatchNote patchNote = patchNote("17.0", true);
        PatchChange buff = patchChange(patchNote, PatchChangeCategory.CHAMPION, PatchChangeType.BUFF, PatchChangeImpact.HIGH, "카이사", 1);
        PatchChange nerf = patchChange(patchNote, PatchChangeCategory.ITEM, PatchChangeType.NERF, PatchChangeImpact.LOW, "죽음검", 2);
        when(patchNoteRepository.findByVersionAndDeletedAtIsNull("17.0"))
                .thenReturn(Optional.of(patchNote));
        givenPatchChangeStats(
                patchNote,
                List.of(
                        categoryChangeCount(PatchChangeCategory.CHAMPION, 1L),
                        categoryChangeCount(PatchChangeCategory.ITEM, 1L)
                ),
                List.of(
                        typeChangeCount(PatchChangeType.BUFF, 1L),
                        typeChangeCount(PatchChangeType.NERF, 1L)
                ),
                1L
        );
        when(patchChangeRepository.findFilteredChanges(
                patchNote,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 1000)
        )).thenReturn(patchChangePage(List.of(buff, nerf), 0, 1000, 2));

        // when
        PatchChangePageResponse response = patchNoteService.getPatchChanges(
                "17.0",
                null,
                null,
                null,
                null,
                1,
                1000
        );

        // then
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getPageSize()).isEqualTo(1000);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }

    @Test
    void 필터_결과가_없으면_빈_items와_정상_페이지_메타를_응답한다() {
        // given
        PatchNote patchNote = patchNote("17.0", true);
        when(patchNoteRepository.findByVersionAndDeletedAtIsNull("17.0"))
                .thenReturn(Optional.of(patchNote));
        givenPatchChangeStats(
                patchNote,
                List.of(categoryChangeCount(PatchChangeCategory.CHAMPION, 1L)),
                List.of(typeChangeCount(PatchChangeType.BUFF, 1L)),
                1L
        );
        when(patchChangeRepository.findFilteredChanges(
                patchNote,
                PatchChangeCategory.ITEM,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        )).thenReturn(patchChangePage(List.of(), 0, 10, 0));

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
        when(patchNoteRepository.findByVersionAndDeletedAtIsNull("17.0"))
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

    private void givenPatchChangeStats(
            PatchNote patchNote,
            List<CategoryChangeCount> categoryCounts,
            List<TypeChangeCount> typeCounts,
            long highImpactCount
    ) {
        givenPatchChangeStats(patchNote, null, null, null, categoryCounts, typeCounts, highImpactCount);
    }

    private void givenPatchChangeStats(
            PatchNote patchNote,
            PatchChangeType changeType,
            PatchChangeImpact impact,
            String query,
            List<CategoryChangeCount> categoryCounts,
            List<TypeChangeCount> typeCounts,
            long highImpactCount
    ) {
        when(patchChangeRepository.countFilteredChangeStats(patchNote, changeType, impact, query))
                .thenReturn(patchChangeStatsCounts(categoryCounts, typeCounts, highImpactCount));
    }

    private List<PatchChangeStatsCount> patchChangeStatsCounts(
            List<CategoryChangeCount> categoryCounts,
            List<TypeChangeCount> typeCounts,
            long highImpactCount
    ) {
        List<PatchChangeStatsCount> statsCounts = new ArrayList<>();
        long remainingHighImpactCount = highImpactCount;
        int itemCount = Math.min(categoryCounts.size(), typeCounts.size());

        for (int index = 0; index < itemCount; index++) {
            CategoryChangeCount categoryCount = categoryCounts.get(index);
            TypeChangeCount typeCount = typeCounts.get(index);
            long changeCount = Math.min(categoryCount.getChangeCount(), typeCount.getChangeCount());
            PatchChangeImpact impact = remainingHighImpactCount > 0
                    ? PatchChangeImpact.HIGH
                    : PatchChangeImpact.LOW;
            statsCounts.add(patchChangeStatsCount(
                    categoryCount.getCategory(),
                    typeCount.getChangeType(),
                    impact,
                    changeCount
            ));
            remainingHighImpactCount -= Math.min(remainingHighImpactCount, changeCount);
        }
        return statsCounts;
    }

    private PageImpl<PatchChange> patchChangePage(
            List<PatchChange> items,
            int page,
            int pageSize,
            long totalItems
    ) {
        return new PageImpl<>(items, PageRequest.of(page, pageSize), totalItems);
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

    private CategoryChangeCount categoryChangeCount(PatchChangeCategory category, Long changeCount) {
        return new CategoryChangeCount() {
            @Override
            public PatchChangeCategory getCategory() {
                return category;
            }

            @Override
            public Long getChangeCount() {
                return changeCount;
            }
        };
    }

    private TypeChangeCount typeChangeCount(PatchChangeType changeType, Long changeCount) {
        return new TypeChangeCount() {
            @Override
            public PatchChangeType getChangeType() {
                return changeType;
            }

            @Override
            public Long getChangeCount() {
                return changeCount;
            }
        };
    }

    private PatchChangeStatsCount patchChangeStatsCount(
            PatchChangeCategory category,
            PatchChangeType changeType,
            PatchChangeImpact impact,
            Long changeCount
    ) {
        return new PatchChangeStatsCount() {
            @Override
            public PatchChangeCategory getCategory() {
                return category;
            }

            @Override
            public PatchChangeType getChangeType() {
                return changeType;
            }

            @Override
            public PatchChangeImpact getImpact() {
                return impact;
            }

            @Override
            public Long getChangeCount() {
                return changeCount;
            }
        };
    }
}
