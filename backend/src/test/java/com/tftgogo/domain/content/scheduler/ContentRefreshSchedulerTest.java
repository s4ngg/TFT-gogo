package com.tftgogo.domain.content.scheduler;

import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.scheduler.GuideCdragonImportTask;
import com.tftgogo.domain.patchnote.config.PatchNoteImportSchedulerProperties;
import com.tftgogo.domain.patchnote.dto.response.AdminPatchNoteImportResponse;
import com.tftgogo.domain.patchnote.scheduler.PatchNoteImportTask;
import com.tftgogo.global.config.GuideCdragonImportProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentRefreshSchedulerTest {

    @Mock
    private PatchNoteImportTask patchNoteImportTask;

    @Mock
    private GuideCdragonImportTask guideCdragonImportTask;

    @Mock
    private ContentRefreshSchedulerLock schedulerLock;

    private PatchNoteImportSchedulerProperties patchNoteProperties;
    private GuideCdragonImportProperties guideProperties;
    private ContentRefreshScheduler scheduler;

    @BeforeEach
    void setUp() {
        patchNoteProperties = new PatchNoteImportSchedulerProperties();
        patchNoteProperties.setEnabled(true);
        guideProperties = new GuideCdragonImportProperties();
        scheduler = new ContentRefreshScheduler(
                patchNoteImportTask,
                guideCdragonImportTask,
                patchNoteProperties,
                guideProperties,
                schedulerLock
        );
    }

    @Test
    void scheduler가_disabled이면_정기_refresh를_실행하지_않는다() {
        // given
        patchNoteProperties.setEnabled(false);

        // when
        scheduler.syncContent();

        // then
        verifyNoInteractions(schedulerLock, patchNoteImportTask, guideCdragonImportTask);
    }

    @Test
    void startup_import가_disabled이면_서버_시작_refresh를_실행하지_않는다() {
        // when
        scheduler.importOnStartupIfEnabled();

        // then
        verifyNoInteractions(schedulerLock, patchNoteImportTask, guideCdragonImportTask);
    }

    @Test
    void guide_source_config가_없으면_patch를_변경하기_전에_전체_refresh를_skip한다() {
        // given
        guideProperties.setEnabled(true);

        // when
        scheduler.syncContent();

        // then
        verify(guideCdragonImportTask).hasExplicitSourceConfiguration();
        verifyNoInteractions(schedulerLock, patchNoteImportTask);
        verify(guideCdragonImportTask, never()).importGuides(anyString(), anyString());
    }

    @Test
    void patch_commit_후_exact_version으로_guide를_순서대로_import한다() {
        // given
        enableGuide();
        givenSchedulerLockRunsTask();
        when(patchNoteImportTask.importLatestPatchNoteThenUnknownPatchNotesFromList())
                .thenReturn(importResponse(" 17.5 "));

        // when
        scheduler.syncContent();

        // then
        InOrder inOrder = inOrder(patchNoteImportTask, guideCdragonImportTask);
        inOrder.verify(patchNoteImportTask).importLatestPatchNoteThenUnknownPatchNotesFromList();
        inOrder.verify(guideCdragonImportTask).importGuides("sync", "17.5");
    }

    @Test
    void daily_refresh도_latest_patch_commit_후_exact_version으로_guide를_import한다() {
        // given
        enableGuide();
        givenSchedulerLockRunsTask();
        when(patchNoteImportTask.importLatestPatchNote()).thenReturn(importResponse("17.5"));

        // when
        scheduler.refreshContent();

        // then
        InOrder inOrder = inOrder(patchNoteImportTask, guideCdragonImportTask);
        inOrder.verify(patchNoteImportTask).importLatestPatchNote();
        inOrder.verify(guideCdragonImportTask).importGuides("daily-refresh", "17.5");
    }

    @Test
    void patch_import가_실패하면_guide를_import하지_않는다() {
        // given
        enableGuide();
        givenSchedulerLockRunsTask();
        when(patchNoteImportTask.importLatestPatchNoteThenUnknownPatchNotesFromList())
                .thenThrow(new RuntimeException("riot unavailable"));

        // when, then
        assertThatCode(scheduler::syncContent).doesNotThrowAnyException();
        verify(guideCdragonImportTask, never()).importGuides(anyString(), anyString());
    }

    @Test
    void commit된_patch_version이_비어있으면_guide를_import하지_않는다() {
        // given
        enableGuide();
        givenSchedulerLockRunsTask();
        when(patchNoteImportTask.importLatestPatchNoteThenUnknownPatchNotesFromList())
                .thenReturn(importResponse("   "));

        // when
        scheduler.syncContent();

        // then
        verify(guideCdragonImportTask).hasExplicitSourceConfiguration();
        verify(guideCdragonImportTask, never()).importGuides(anyString(), anyString());
    }

    @Test
    void guide_import_실패후_다음_실행에서_재시도한다() {
        // given
        enableGuide();
        givenSchedulerLockRunsTask();
        when(patchNoteImportTask.importLatestPatchNoteThenUnknownPatchNotesFromList())
                .thenReturn(importResponse("17.5"));
        when(guideCdragonImportTask.importGuides("sync", "17.5"))
                .thenThrow(new RuntimeException("cdragon unavailable"))
                .thenReturn(mock(GuideImportResponse.class));

        // when
        scheduler.syncContent();
        scheduler.syncContent();

        // then
        verify(patchNoteImportTask, times(2)).importLatestPatchNoteThenUnknownPatchNotesFromList();
        verify(guideCdragonImportTask, times(2)).importGuides("sync", "17.5");
    }

    @Test
    void 같은_서버에서_실행중_재진입하면_추가_task를_실행하지_않는다() {
        // given
        givenSchedulerLockRunsTask();
        when(patchNoteImportTask.importLatestPatchNoteThenUnknownPatchNotesFromList())
                .thenAnswer(invocation -> {
                    scheduler.syncContent();
                    return importResponse("17.5");
                });

        // when
        scheduler.syncContent();

        // then
        verify(schedulerLock).runWithLock(anyString(), any(Runnable.class));
        verify(patchNoteImportTask).importLatestPatchNoteThenUnknownPatchNotesFromList();
    }

    @Test
    void DB_락을_얻지_못하면_patch와_guide_task를_실행하지_않는다() {
        // given
        when(schedulerLock.runWithLock(anyString(), any(Runnable.class))).thenReturn(false);

        // when
        scheduler.syncContent();

        // then
        verifyNoInteractions(patchNoteImportTask, guideCdragonImportTask);
    }

    @Test
    void 서로_다른_scheduler가_공유_락에서_경쟁하면_한쪽만_task를_실행한다() throws Exception {
        // given
        ContentRefreshScheduler otherScheduler = new ContentRefreshScheduler(
                patchNoteImportTask,
                guideCdragonImportTask,
                patchNoteProperties,
                guideProperties,
                schedulerLock
        );
        AtomicBoolean lockHeld = new AtomicBoolean(false);
        CountDownLatch firstTaskStarted = new CountDownLatch(1);
        CountDownLatch allowFirstTaskToFinish = new CountDownLatch(1);
        doAnswer(invocation -> {
            if (!lockHeld.compareAndSet(false, true)) {
                return false;
            }
            try {
                Runnable task = invocation.getArgument(1);
                task.run();
                return true;
            } finally {
                lockHeld.set(false);
            }
        }).when(schedulerLock).runWithLock(anyString(), any(Runnable.class));
        when(patchNoteImportTask.importLatestPatchNoteThenUnknownPatchNotesFromList())
                .thenAnswer(invocation -> {
                    firstTaskStarted.countDown();
                    allowFirstTaskToFinish.await(2, TimeUnit.SECONDS);
                    return importResponse("17.5");
                });

        Thread firstServer = new Thread(scheduler::syncContent);
        firstServer.start();
        try {
            assertThat(firstTaskStarted.await(1, TimeUnit.SECONDS)).isTrue();

            // when
            otherScheduler.syncContent();
        } finally {
            allowFirstTaskToFinish.countDown();
            firstServer.join(2_000);
        }

        // then
        assertThat(firstServer.isAlive()).isFalse();
        verify(schedulerLock, times(2)).runWithLock(anyString(), any(Runnable.class));
        verify(patchNoteImportTask).importLatestPatchNoteThenUnknownPatchNotesFromList();
    }

    private void enableGuide() {
        guideProperties.setEnabled(true);
        when(guideCdragonImportTask.hasExplicitSourceConfiguration()).thenReturn(true);
    }

    private void givenSchedulerLockRunsTask() {
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return true;
        }).when(schedulerLock).runWithLock(anyString(), any(Runnable.class));
    }

    private AdminPatchNoteImportResponse importResponse(String version) {
        return AdminPatchNoteImportResponse.of(
                1L,
                version,
                "https://example.com/patch-note",
                true,
                false,
                false,
                1,
                0,
                0,
                List.of()
        );
    }
}
