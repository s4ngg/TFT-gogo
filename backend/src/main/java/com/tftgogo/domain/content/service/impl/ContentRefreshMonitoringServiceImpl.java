package com.tftgogo.domain.content.service.impl;

import com.tftgogo.domain.content.config.ContentRefreshMonitoringProperties;
import com.tftgogo.domain.content.dto.response.ContentRefreshHealthResponse;
import com.tftgogo.domain.content.entity.ContentRefreshFailureType;
import com.tftgogo.domain.content.entity.ContentRefreshJobStatus;
import com.tftgogo.domain.content.entity.ContentRefreshJobType;
import com.tftgogo.domain.content.entity.ContentRefreshRunStatus;
import com.tftgogo.domain.content.repository.ContentRefreshJobStatusRepository;
import com.tftgogo.domain.content.service.ContentRefreshMonitoringService;
import com.tftgogo.domain.guide.entity.GuideSnapshot;
import com.tftgogo.domain.guide.entity.GuideSnapshotStatus;
import com.tftgogo.domain.guide.repository.GuideSnapshotRepository;
import com.tftgogo.domain.guide.repository.GuideSnapshotRepository.GuideDataCounts;
import com.tftgogo.domain.patchnote.config.PatchNoteImportSchedulerProperties;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.global.config.GuideCdragonImportProperties;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentRefreshMonitoringServiceImpl implements ContentRefreshMonitoringService {

    private static final String DISABLED = "DISABLED";
    private static final String HEALTHY = "HEALTHY";
    private static final String RUNNING = "RUNNING";
    private static final String WARNING = "WARNING";
    private static final String CRITICAL = "CRITICAL";

    private final ContentRefreshJobStatusRepository statusRepository;
    private final PatchNoteRepository patchNoteRepository;
    private final PatchChangeRepository patchChangeRepository;
    private final GuideSnapshotRepository guideSnapshotRepository;
    private final PatchNoteImportSchedulerProperties patchNoteProperties;
    private final GuideCdragonImportProperties guideProperties;
    private final ContentRefreshMonitoringProperties monitoringProperties;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(ContentRefreshJobType jobType) {
        ContentRefreshJobStatus status = findOrCreate(jobType);
        status.recordAttempt(LocalDateTime.now());
        statusRepository.save(status);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(ContentRefreshJobType jobType, String version, long processedCount) {
        ContentRefreshJobStatus status = findOrCreate(jobType);
        status.recordSuccess(version, processedCount, LocalDateTime.now());
        statusRepository.save(status);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(ContentRefreshJobType jobType, Throwable failure) {
        ContentRefreshJobStatus status = findOrCreate(jobType);
        status.recordFailure(classifyFailure(failure), LocalDateTime.now());
        statusRepository.save(status);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPartialSuccess(
            ContentRefreshJobType jobType,
            String version,
            long processedCount,
            ContentRefreshFailureType failureType
    ) {
        ContentRefreshJobStatus status = findOrCreate(jobType);
        status.recordPartialSuccess(version, processedCount, failureType, LocalDateTime.now());
        statusRepository.save(status);
    }

    @Override
    public ContentRefreshHealthResponse getHealth() {
        LocalDateTime now = LocalDateTime.now();
        Map<ContentRefreshJobType, ContentRefreshJobStatus> statuses = new EnumMap<>(ContentRefreshJobType.class);
        statusRepository.findAll().forEach(status -> statuses.put(status.getJobType(), status));

        Optional<PatchNote> currentPatch = patchNoteRepository
                .findFirstByCurrentTrueAndDeletedAtIsNullOrderByPublishedAtDescIdDesc();
        Long currentPatchCount = currentPatch
                .map(patchChangeRepository::countByPatchNote)
                .orElse(null);
        String currentPatchVersion = currentPatch.map(PatchNote::getVersion).orElse(null);

        Optional<GuideSnapshot> activeGuide = guideSnapshotRepository
                .findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE);
        GuideDataCounts activeGuideDataCounts = activeGuide
                .map(snapshot -> guideSnapshotRepository.countGuideDataByPatchVersion(snapshot.getPatchVersion()))
                .orElse(null);
        Long currentGuideCount = activeGuideDataCounts == null ? null : totalGuideCount(activeGuideDataCounts);
        String currentGuideVersion = activeGuide.map(GuideSnapshot::getPatchVersion).orElse(null);

        ContentRefreshHealthResponse.JobHealth patchHealth = buildJobHealth(
                ContentRefreshJobType.PATCH_NOTE,
                patchNoteProperties.isEnabled(),
                statuses.get(ContentRefreshJobType.PATCH_NOTE),
                now,
                currentPatchVersion,
                currentPatchCount,
                currentPatchCount != null && currentPatchCount > 0,
                patchAlertReasons(currentPatch, currentPatchCount)
        );
        ContentRefreshHealthResponse.JobHealth guideHealth = buildJobHealth(
                ContentRefreshJobType.GAME_GUIDE,
                patchNoteProperties.isEnabled() && guideProperties.isEnabled(),
                statuses.get(ContentRefreshJobType.GAME_GUIDE),
                now,
                currentGuideVersion,
                currentGuideCount,
                activeGuide.isPresent() && isCompleteGuideSnapshot(activeGuide.get(), activeGuideDataCounts),
                guideAlertReasons(activeGuide, activeGuideDataCounts, currentPatchVersion)
        );

        List<ContentRefreshHealthResponse.JobHealth> jobs = List.of(patchHealth, guideHealth);
        return ContentRefreshHealthResponse.of(
                overallStatus(jobs),
                now,
                monitoringProperties.getStaleAfterHours(),
                monitoringProperties.getConsecutiveFailureCriticalThreshold(),
                monitoringProperties.getRunningTimeoutMinutes(),
                jobs
        );
    }

    private ContentRefreshJobStatus findOrCreate(ContentRefreshJobType jobType) {
        if (jobType == null) {
            throw new IllegalArgumentException("Content refresh job type is required");
        }
        return statusRepository.findById(jobType)
                .orElseGet(() -> ContentRefreshJobStatus.create(jobType));
    }

    private ContentRefreshHealthResponse.JobHealth buildJobHealth(
            ContentRefreshJobType jobType,
            boolean enabled,
            ContentRefreshJobStatus status,
            LocalDateTime now,
            String currentVersion,
            Long currentCount,
            boolean currentDataComplete,
            List<String> dataAlertReasons
    ) {
        ContentRefreshRunStatus lastStatus = status == null
                ? ContentRefreshRunStatus.NEVER_RUN
                : status.getLastStatus();
        LocalDateTime lastAttemptAt = status == null ? null : status.getLastAttemptAt();
        LocalDateTime lastSuccessAt = status == null ? null : status.getLastSuccessAt();
        int consecutiveFailures = status == null ? 0 : status.getConsecutiveFailureCount();
        boolean fresh = lastSuccessAt != null
                && !lastSuccessAt.isBefore(now.minusHours(monitoringProperties.getStaleAfterHours()));

        List<String> alertReasons = new ArrayList<>();
        if (enabled) {
            boolean activelyRunning = lastStatus == ContentRefreshRunStatus.RUNNING
                    && lastAttemptAt != null
                    && !lastAttemptAt.isBefore(now.minusMinutes(monitoringProperties.getRunningTimeoutMinutes()));
            if (!activelyRunning && lastSuccessAt == null) {
                alertReasons.add("NEVER_SUCCEEDED");
            } else if (lastSuccessAt != null && !fresh) {
                alertReasons.add("STALE");
            }
            if (lastStatus == ContentRefreshRunStatus.RUNNING
                    && lastAttemptAt != null
                    && lastAttemptAt.isBefore(now.minusMinutes(monitoringProperties.getRunningTimeoutMinutes()))) {
                alertReasons.add("STUCK_RUNNING");
            }
            if (consecutiveFailures >= monitoringProperties.getConsecutiveFailureCriticalThreshold()) {
                alertReasons.add("CONSECUTIVE_FAILURES");
            } else if (consecutiveFailures > 0) {
                alertReasons.add("RECENT_FAILURE");
            }
            alertReasons.addAll(dataAlertReasons);
        }

        String healthStatus = resolveJobStatus(enabled, lastStatus, alertReasons);
        return ContentRefreshHealthResponse.JobHealth.of(
                jobType,
                enabled,
                healthStatus,
                alertReasons,
                lastStatus,
                lastAttemptAt,
                lastSuccessAt,
                status == null ? null : status.getLastFailureAt(),
                status == null ? null : status.getLastSuccessVersion(),
                status == null ? null : status.getLastSuccessCount(),
                consecutiveFailures,
                status == null ? null : status.getLastFailureType(),
                fresh,
                currentVersion,
                currentCount,
                currentDataComplete
        );
    }

    private List<String> patchAlertReasons(Optional<PatchNote> currentPatch, Long currentPatchCount) {
        if (currentPatch.isEmpty()) {
            return List.of("CURRENT_PATCH_MISSING");
        }
        if (currentPatchCount == null || currentPatchCount == 0) {
            return List.of("CURRENT_PATCH_CHANGES_EMPTY");
        }
        return List.of();
    }

    private List<String> guideAlertReasons(
            Optional<GuideSnapshot> activeGuide,
            GuideDataCounts actualCounts,
            String currentPatchVersion
    ) {
        List<String> reasons = new ArrayList<>();
        if (!hasValidGuideConfiguration()) {
            reasons.add("GUIDE_CONFIGURATION_INVALID");
        }
        if (activeGuide.isEmpty()) {
            reasons.add("ACTIVE_GUIDE_SNAPSHOT_MISSING");
            return reasons;
        }

        GuideSnapshot snapshot = activeGuide.get();
        if (snapshot.getValidatedAt() == null) {
            reasons.add("ACTIVE_GUIDE_SNAPSHOT_UNVALIDATED");
        }
        if (!isCompleteGuideSnapshot(snapshot, actualCounts)) {
            reasons.add("ACTIVE_GUIDE_SNAPSHOT_INCOMPLETE");
        }
        if (actualCounts != null && !matchesSnapshotCounts(snapshot, actualCounts)) {
            reasons.add("ACTIVE_GUIDE_COUNT_MISMATCH");
        }
        if (currentPatchVersion != null && !currentPatchVersion.equals(snapshot.getPatchVersion())) {
            reasons.add("PATCH_GUIDE_VERSION_MISMATCH");
        }
        return reasons;
    }

    private boolean hasValidGuideConfiguration() {
        String mutator = guideProperties.getMutator();
        return guideProperties.getSetNumber() != null
                && guideProperties.getSetNumber() > 0
                && mutator != null
                && !mutator.trim().isEmpty()
                && mutator.trim().length() <= 100
                && guideProperties.isIncludeChampions()
                && guideProperties.isIncludeTraits()
                && guideProperties.isIncludeItems()
                && guideProperties.isIncludeAugments();
    }

    private boolean isCompleteGuideSnapshot(GuideSnapshot snapshot, GuideDataCounts actualCounts) {
        return snapshot.getValidatedAt() != null
                && actualCounts != null
                && actualCounts.getChampionCount() >= minimumRequired(guideProperties.getMinimumChampionCount())
                && actualCounts.getTraitCount() >= minimumRequired(guideProperties.getMinimumTraitCount())
                && actualCounts.getItemCount() >= minimumRequired(guideProperties.getMinimumItemCount())
                && actualCounts.getAugmentCount() >= minimumRequired(guideProperties.getMinimumAugmentCount());
    }

    private int minimumRequired(int configuredMinimum) {
        return Math.max(1, configuredMinimum);
    }

    private boolean matchesSnapshotCounts(GuideSnapshot snapshot, GuideDataCounts actualCounts) {
        return snapshot.getChampionCount() == actualCounts.getChampionCount()
                && snapshot.getTraitCount() == actualCounts.getTraitCount()
                && snapshot.getItemCount() == actualCounts.getItemCount()
                && snapshot.getAugmentCount() == actualCounts.getAugmentCount();
    }

    private long totalGuideCount(GuideDataCounts counts) {
        return counts.getChampionCount()
                + counts.getTraitCount()
                + counts.getItemCount()
                + counts.getAugmentCount();
    }

    private String resolveJobStatus(
            boolean enabled,
            ContentRefreshRunStatus lastStatus,
            List<String> alertReasons
    ) {
        if (!enabled) {
            return DISABLED;
        }
        boolean critical = alertReasons.stream().anyMatch(reason -> !"RECENT_FAILURE".equals(reason));
        if (critical) {
            return CRITICAL;
        }
        if (alertReasons.contains("RECENT_FAILURE")) {
            return WARNING;
        }
        if (lastStatus == ContentRefreshRunStatus.RUNNING) {
            return RUNNING;
        }
        return HEALTHY;
    }

    private String overallStatus(List<ContentRefreshHealthResponse.JobHealth> jobs) {
        if (jobs.stream().anyMatch(job -> CRITICAL.equals(job.getStatus()))) {
            return CRITICAL;
        }
        if (jobs.stream().anyMatch(job -> WARNING.equals(job.getStatus()))) {
            return WARNING;
        }
        if (jobs.stream().anyMatch(job -> RUNNING.equals(job.getStatus()))) {
            return RUNNING;
        }
        if (jobs.stream().allMatch(job -> DISABLED.equals(job.getStatus()))) {
            return DISABLED;
        }
        return HEALTHY;
    }

    private ContentRefreshFailureType classifyFailure(Throwable failure) {
        if (failure == null) {
            return ContentRefreshFailureType.UNEXPECTED;
        }
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof BusinessException businessException) {
                return classifyErrorCode(businessException.getErrorCode());
            }
            if (current instanceof DataAccessException) {
                return ContentRefreshFailureType.STORAGE;
            }
            if (current instanceof RestClientException) {
                return ContentRefreshFailureType.SOURCE_UNAVAILABLE;
            }
            if (current instanceof IllegalArgumentException) {
                return ContentRefreshFailureType.VALIDATION;
            }
            if (current instanceof IllegalStateException) {
                return ContentRefreshFailureType.INVALID_DATA;
            }
        }
        return ContentRefreshFailureType.UNEXPECTED;
    }

    private ContentRefreshFailureType classifyErrorCode(ErrorCode errorCode) {
        return switch (errorCode) {
            case PATCH_NOTE_INVALID_DATA, GUIDE_INVALID_DATA -> ContentRefreshFailureType.INVALID_DATA;
            case INVALID_INPUT -> ContentRefreshFailureType.VALIDATION;
            case EXTERNAL_API_ERROR, RIOT_API_ERROR, RIOT_API_TIMEOUT, RIOT_API_RATE_LIMIT ->
                    ContentRefreshFailureType.SOURCE_UNAVAILABLE;
            case CONTENT_REFRESH_ALREADY_RUNNING -> ContentRefreshFailureType.CONCURRENCY;
            default -> ContentRefreshFailureType.UNEXPECTED;
        };
    }
}
