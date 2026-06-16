package com.tftgogo.domain.patchnote.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchChangeRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteRequest;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeCategory;
import com.tftgogo.domain.patchnote.entity.PatchChangeImpact;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.entity.PatchNoteImportSource;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPatchNoteServiceImplTest {

    @Mock
    private PatchNoteRepository patchNoteRepository;

    @Mock
    private PatchChangeRepository patchChangeRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AdminPatchNoteServiceImpl adminPatchNoteService;

    @Test
    void createPatchNote_whenCurrentTrue_clearsExistingCurrentPatch() {
        // given
        PatchNote existingCurrent = patchNote(1L, "17.3", true);
        AdminPatchNoteRequest request = patchNoteRequest("17.4", true, List.of("current patch"));
        when(patchNoteRepository.findByVersion("17.4")).thenReturn(Optional.empty());
        when(patchNoteRepository.findByCurrentTrueAndDeletedAtIsNull())
                .thenReturn(List.of(existingCurrent));
        when(patchNoteRepository.save(any(PatchNote.class))).thenAnswer(invocation -> {
            PatchNote patchNote = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchNote, "id", 2L);
            return patchNote;
        });

        // when
        PatchNoteResponse response = adminPatchNoteService.createPatchNote(request);

        // then
        assertThat(existingCurrent.isCurrent()).isFalse();
        assertThat(response.getVersion()).isEqualTo("17.4");
        assertThat(response.getIsCurrent()).isTrue();
        assertThat(response.getHighlights()).containsExactly("current patch");

        ArgumentCaptor<PatchNote> captor = ArgumentCaptor.forClass(PatchNote.class);
        verify(patchNoteRepository).save(captor.capture());
        assertThat(captor.getValue().getHighlightsJson()).isEqualTo("[\"current patch\"]");
    }

    @Test
    void createPatchNote_whenDescriptionBlank_usesSummaryAsContent() {
        // given
        AdminPatchNoteRequest request = patchNoteRequest("17.4", false, List.of());
        ReflectionTestUtils.setField(request, "description", " ");
        when(patchNoteRepository.findByVersion("17.4")).thenReturn(Optional.empty());
        when(patchNoteRepository.save(any(PatchNote.class))).thenAnswer(invocation -> {
            PatchNote patchNote = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchNote, "id", 2L);
            return patchNote;
        });

        // when
        adminPatchNoteService.createPatchNote(request);

        // then
        ArgumentCaptor<PatchNote> captor = ArgumentCaptor.forClass(PatchNote.class);
        verify(patchNoteRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("summary");
    }

    @Test
    void updatePatchNote_whenVersionAlreadyExists_throwsInvalidInput() {
        // given
        PatchNote target = patchNote(1L, "17.3", true);
        PatchNote duplicate = patchNote(2L, "17.4", false);
        AdminPatchNoteRequest request = patchNoteRequest("17.4", false, List.of());
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(target));
        when(patchNoteRepository.findByVersion("17.4")).thenReturn(Optional.of(duplicate));

        // when, then
        assertThatThrownBy(() -> adminPatchNoteService.updatePatchNote(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void updatePatchNote_whenImported_marksManuallyEdited() {
        // given
        PatchNote patchNote = importedPatchNote(1L, "17.3", true);
        AdminPatchNoteRequest request = patchNoteRequest("17.3", true, List.of("manual highlight"));
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));
        when(patchNoteRepository.findByVersion("17.3")).thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(patchNote))
                .thenReturn(List.of());

        // when
        PatchNoteResponse response = adminPatchNoteService.updatePatchNote(1L, request);

        // then
        assertThat(response.getHighlights()).containsExactly("manual highlight");
        assertThat(patchNote.isManuallyEdited()).isTrue();
    }

    @Test
    void deletePatchNote_softDeletesPatchNoteAndChanges() {
        // given
        PatchNote patchNote = patchNote(1L, "17.3", true);
        PatchChange patchChange = patchChange(10L, patchNote);
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(patchNote))
                .thenReturn(List.of(patchChange));

        // when
        adminPatchNoteService.deletePatchNote(1L);

        // then
        assertThat(patchNote.isCurrent()).isFalse();
        assertThat(patchNote.getDeletedAt()).isNotNull();
        verify(patchChangeRepository).delete(patchChange);
    }

    @Test
    void deletePatchNote_whenImported_marksPatchNoteAndChangesManuallyEdited() {
        // given
        PatchNote patchNote = importedPatchNote(1L, "17.3", true);
        PatchChange patchChange = importedPatchChange(10L, patchNote);
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(patchNote))
                .thenReturn(List.of(patchChange));

        // when
        adminPatchNoteService.deletePatchNote(1L);

        // then
        assertThat(patchNote.isManuallyEdited()).isTrue();
        assertThat(patchChange.isManuallyEdited()).isTrue();
        assertThat(patchNote.getDeletedAt()).isNotNull();
        verify(patchChangeRepository).delete(patchChange);
    }

    @Test
    void getPatchChanges_returnsNonDeletedChangesForAdmin() {
        // given
        PatchNote patchNote = patchNote(1L, "17.3", true);
        PatchChange firstChange = patchChange(10L, patchNote, 1);
        PatchChange hiddenChange = patchChange(11L, patchNote, 2);
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(patchNote))
                .thenReturn(List.of(firstChange, hiddenChange));

        // when
        List<PatchChangeResponse> responses = adminPatchNoteService.getPatchChanges(1L);

        // then
        assertThat(responses).extracting(PatchChangeResponse::getId).containsExactly(10L, 11L);
        assertThat(responses).extracting(PatchChangeResponse::getSortOrder).containsExactly(1, 2);
        verify(patchChangeRepository).findByPatchNoteOrderBySortOrderAscIdAsc(patchNote);
    }

    @Test
    void createPatchChange_whenCategoryInvalid_throwsInvalidInput() {
        // given
        PatchNote patchNote = patchNote(1L, "17.3", true);
        AdminPatchChangeRequest request = patchChangeRequest(1L, "UNKNOWN");
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));

        // when, then
        assertThatThrownBy(() -> adminPatchNoteService.createPatchChange(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(patchChangeRepository, never()).save(any(PatchChange.class));
    }

    @Test
    void updatePatchChange_whenImported_marksManuallyEdited() {
        // given
        PatchNote patchNote = patchNote(1L, "17.3", true);
        PatchChange patchChange = importedPatchChange(10L, patchNote);
        AdminPatchChangeRequest request = patchChangeRequest(1L, "CHAMPION");
        when(patchChangeRepository.findById(10L)).thenReturn(Optional.of(patchChange));
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));

        // when
        PatchChangeResponse response = adminPatchNoteService.updatePatchChange(10L, request);

        // then
        assertThat(response.getTargetName()).isEqualTo("Jinx");
        assertThat(patchChange.isManuallyEdited()).isTrue();
    }

    private PatchNote patchNote(Long id, String version, boolean current) {
        PatchNote patchNote = PatchNote.builder()
                .version(version)
                .title(version + " patch")
                .summary("summary")
                .description("description")
                .focus("balance")
                .imageUrl("https://example.com/patch.png")
                .publishedAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                .current(current)
                .highlightsJson("[\"highlight\"]")
                .build();
        ReflectionTestUtils.setField(patchNote, "id", id);
        return patchNote;
    }

    private PatchNote importedPatchNote(Long id, String version, boolean current) {
        PatchNote patchNote = PatchNote.builder()
                .version(version)
                .title(version + " patch")
                .summary("summary")
                .description("description")
                .focus("balance")
                .imageUrl("https://example.com/patch.png")
                .sourceUrl("https://www.leagueoflegends.com/ko-kr/news/game-updates/patch-17-3-notes/")
                .sourceLocale("ko_KR")
                .importSource(PatchNoteImportSource.RIOT_OFFICIAL)
                .importedAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .publishedAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                .current(current)
                .highlightsJson("[\"highlight\"]")
                .build();
        ReflectionTestUtils.setField(patchNote, "id", id);
        return patchNote;
    }

    private PatchChange patchChange(Long id, PatchNote patchNote) {
        return patchChange(id, patchNote, 1);
    }

    private PatchChange patchChange(Long id, PatchNote patchNote, int sortOrder) {
        PatchChange patchChange = PatchChange.builder()
                .patchNote(patchNote)
                .category(PatchChangeCategory.CHAMPION)
                .changeType(PatchChangeType.BUFF)
                .impact(PatchChangeImpact.HIGH)
                .targetKey("tft17_jinx")
                .targetName("Jinx")
                .summary("Jinx buff")
                .beforeValue("10")
                .afterValue("20")
                .imageUrl("https://example.com/jinx.png")
                .tagsJson("[\"champion\"]")
                .sortOrder(sortOrder)
                .build();
        ReflectionTestUtils.setField(patchChange, "id", id);
        return patchChange;
    }

    private PatchChange importedPatchChange(Long id, PatchNote patchNote) {
        PatchChange patchChange = PatchChange.builder()
                .patchNote(patchNote)
                .sourceKey("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .sourceUrl("https://www.leagueoflegends.com/ko-kr/news/game-updates/patch-17-3-notes/")
                .sourceHeadingPath("Champion > Jinx")
                .sourceOrder(1)
                .sourceLocale("ko_KR")
                .importSource(PatchNoteImportSource.RIOT_OFFICIAL)
                .importedAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .category(PatchChangeCategory.CHAMPION)
                .changeType(PatchChangeType.BUFF)
                .impact(PatchChangeImpact.HIGH)
                .targetKey("tft17_jinx")
                .targetName("Jinx")
                .summary("Jinx buff")
                .beforeValue("10")
                .afterValue("20")
                .imageUrl("https://example.com/jinx.png")
                .tagsJson("[\"champion\"]")
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(patchChange, "id", id);
        return patchChange;
    }

    private AdminPatchNoteRequest patchNoteRequest(String version, boolean current, List<String> highlights) {
        AdminPatchNoteRequest request = new AdminPatchNoteRequest();
        ReflectionTestUtils.setField(request, "version", version);
        ReflectionTestUtils.setField(request, "title", version + " patch");
        ReflectionTestUtils.setField(request, "summary", "summary");
        ReflectionTestUtils.setField(request, "description", "description");
        ReflectionTestUtils.setField(request, "focus", "balance");
        ReflectionTestUtils.setField(request, "imageUrl", "https://example.com/patch.png");
        ReflectionTestUtils.setField(request, "publishedAt", LocalDateTime.of(2026, 6, 1, 9, 0));
        ReflectionTestUtils.setField(request, "current", current);
        ReflectionTestUtils.setField(request, "highlights", highlights);
        return request;
    }

    private AdminPatchChangeRequest patchChangeRequest(Long patchNoteId, String category) {
        AdminPatchChangeRequest request = new AdminPatchChangeRequest();
        ReflectionTestUtils.setField(request, "patchNoteId", patchNoteId);
        ReflectionTestUtils.setField(request, "category", category);
        ReflectionTestUtils.setField(request, "type", "BUFF");
        ReflectionTestUtils.setField(request, "impact", "HIGH");
        ReflectionTestUtils.setField(request, "targetKey", "tft17_jinx");
        ReflectionTestUtils.setField(request, "targetName", "Jinx");
        ReflectionTestUtils.setField(request, "summary", "Jinx buff");
        ReflectionTestUtils.setField(request, "beforeValue", "10");
        ReflectionTestUtils.setField(request, "afterValue", "20");
        ReflectionTestUtils.setField(request, "imageUrl", "https://example.com/jinx.png");
        ReflectionTestUtils.setField(request, "tags", List.of("champion"));
        ReflectionTestUtils.setField(request, "sortOrder", 1);
        return request;
    }
}
