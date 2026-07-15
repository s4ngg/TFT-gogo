package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.entity.GuideAugment;
import com.tftgogo.domain.guide.entity.GuideChampion;
import com.tftgogo.domain.guide.entity.GuideItem;
import com.tftgogo.domain.guide.entity.GuideSnapshot;
import com.tftgogo.domain.guide.entity.GuideSnapshotStatus;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.GuideAugmentRepository;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideItemRepository;
import com.tftgogo.domain.guide.repository.GuideSnapshotRepository;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.domain.guide.service.GuideCdragonImportService;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.global.cdragon.config.CommunityDragonProperties;
import com.tftgogo.global.config.GuideCdragonImportProperties;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GuideCdragonImportServiceImpl implements GuideCdragonImportService {

    private static final Logger logger = LogManager.getLogger(GuideCdragonImportServiceImpl.class);
    private static final Pattern PATCH_VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(.*)$");
    private static final Pattern BREAK_TAG_PATTERN = Pattern.compile("(?i)<br\\s*/?>|</p>|</li>|</div>");
    private static final Pattern ROW_TAG_PATTERN = Pattern.compile("(?is)<(row|expandRow)>(.*?)</\\1>");
    private static final Pattern STARGAZER_VARIANT_PATTERN = Pattern.compile("이번 게임:\\s*([^\\s(]+)");
    private static final Pattern SHOW_IF_NOT_BLOCK_PATTERN =
            Pattern.compile("(?is)<ShowIfNot\\.[^>]*>.*?</ShowIfNot\\.[^>]*>");
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("@([^@]+)@");
    private static final Pattern MULTIPLY_EXPRESSION_PATTERN =
            Pattern.compile("^([A-Za-z0-9_]+)\\s*\\*\\s*([0-9]+(?:\\.[0-9]+)?)$");
    private static final Pattern CDRAGON_TOKEN_PATTERN = Pattern.compile("%[A-Za-z_:][A-Za-z0-9_:.-]*%");
    private static final Pattern DOUBLE_BRACE_TOKEN_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");
    private static final Pattern HASH_TAG_PATTERN = Pattern.compile("^\\{[0-9a-fA-F]{6,}\\}$");
    private static final Pattern STANDALONE_PERCENT_PATTERN = Pattern.compile("(^|[^0-9])%");
    private static final Pattern METRIC_ONLY_PATTERN = Pattern.compile("^[+\\-]?\\d[\\d,./%\\s+\\-]*$");
    private static final Pattern EMPTY_PARENS_PATTERN = Pattern.compile("\\(\\s*\\)");
    private static final int PATCH_VERSION_MAX_LENGTH = 20;
    private static final int MUTATOR_MAX_LENGTH = 100;
    private static final String LATEST_PATCH_VERSION_ALIAS = "latest";
    private static final Set<String> SPECIAL_UNIT_KEYS = Set.of(
            "TFT17_DarkStar_FakeUnit"
    );
    private static final Map<String, Set<String>> TRAIT_SPECIAL_UNIT_KEYS = Map.of(
            "TFT17_DarkStar", Set.of("TFT17_DarkStar_FakeUnit")
    );
    private static final int AUGMENT_TAG_LIMIT = 4;
    private static final String[] AUGMENT_REROLL_KEYWORDS =
            {"새로고침", "상점", "주사위", "reroll", "refresh", "shop", "dice", "roll"};
    private static final String[] AUGMENT_ECONOMY_KEYWORDS =
            {"골드", "동전", "경험치", "황금", "gold", "coin", "xp", "experience", "golden"};
    private static final String[] AUGMENT_ITEM_TAG_KEYWORDS =
            {"아이템", "모루", "상자", "찬란", "장갑", "활", "item", "anvil", "glove", "bow"};
    private static final String[] AUGMENT_CHAMPION_KEYWORDS =
            {"챔피언", "유닛", "단계", "champion", "unit"};
    private static final String[] AUGMENT_SYNERGY_KEYWORDS =
            {"상징", "문장", "특성", "emblem", "trait"};
    private static final String[] AUGMENT_QUEST_KEYWORDS =
            {"퀘스트", "quest"};
    private static final String[] AUGMENT_COMBAT_TAG_KEYWORDS = {
            "공격력",
            "주문력",
            "체력",
            "방어력",
            "마법 저항력",
            "공격 속도",
            "피해",
            "보호막",
            "마나",
            "치명타",
            "attack",
            "ability power",
            "health",
            "armor",
            "magic resist",
            "attack speed",
            "damage",
            "shield",
            "mana",
            "critical"
    };
    private static final Map<String, String> CDRAGON_ICON_LABELS = Map.ofEntries(
            Map.entry("scaleas", "공격 속도"),
            Map.entry("scalead", "공격력"),
            Map.entry("scaleap", "주문력"),
            Map.entry("scalehealth", "체력"),
            Map.entry("scalehp", "체력"),
            Map.entry("scalearmor", "방어력"),
            Map.entry("scalemr", "마법 저항력"),
            Map.entry("scalemagicresist", "마법 저항력"),
            Map.entry("scalemana", "마나"),
            Map.entry("scalecrit", "치명타"),
            Map.entry("scalecritchance", "치명타 확률"),
            Map.entry("gold", "골드"),
            Map.entry("range", "사거리")
    );
    private static final Map<String, String> CDRAGON_TEMPLATE_TOKEN_LABELS = Map.ofEntries(
            Map.entry("TFT_Keyword_Precision", "정밀: 스킬과 아이템 피해가 치명타로 적용될 수 있습니다.")
    );
    private final GuideChampionRepository guideChampionRepository;
    private final GuideTraitRepository guideTraitRepository;
    private final GuideItemRepository guideItemRepository;
    private final GuideAugmentRepository guideAugmentRepository;
    private final GuideSnapshotRepository guideSnapshotRepository;
    private final PatchNoteRepository patchNoteRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CommunityDragonProperties communityDragonProperties;
    private final GuideCdragonImportProperties guideCdragonImportProperties;

    @Override
    @Transactional
    public GuideImportResponse importGuides(GuideCdragonImportRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (!request.shouldIncludeChampions()
                && !request.shouldIncludeTraits()
                && !request.shouldIncludeItems()
                && !request.shouldIncludeAugments()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        GuideImportSource requestedSource = resolveRequestedSource(request.getSetNumber(), request.resolveMutator());
        String patchVersion = normalizePatchVersion(request.getPatchVersion());
        validateConfiguredPatchSource(request.getPatchVersion(), patchVersion, requestedSource);
        JsonNode cdragonData = fetchCdragonData();
        ResolvedCdragonSet resolvedSet = resolveSetData(
                cdragonData,
                requestedSource.setNumber(),
                requestedSource.mutator()
        );
        JsonNode setData = resolvedSet.data();
        JsonNode items = cdragonData.path("items");
        List<JsonNode> allChampions = readSetChampions(setData);
        List<JsonNode> champions = readShopChampions(allChampions, resolvedSet.setNumber());
        List<GuideCandidate> candidates = new ArrayList<>();
        if (request.shouldIncludeChampions()) {
            candidates.addAll(toChampionCandidates(champions, patchVersion));
        }
        if (request.shouldIncludeTraits()) {
            candidates.addAll(toTraitCandidates(setData.path("traits"), champions, allChampions, patchVersion));
        }
        if (request.shouldIncludeItems()) {
            candidates.addAll(toItemCandidates(items, patchVersion));
        }
        if (request.shouldIncludeAugments()) {
            candidates.addAll(toAugmentCandidates(readAugments(setData.path("augments"), items), patchVersion));
        }

        GuideImportCounts counts = new GuideImportCounts(
                countByType(candidates, GuideType.CHAMPION),
                countByType(candidates, GuideType.TRAIT),
                countByType(candidates, GuideType.ITEM),
                countByType(candidates, GuideType.AUGMENT)
        );
        validateRequestedImport(request, counts);
        boolean completeImport = isCompleteImport(request);
        Optional<GuideSnapshot> targetSnapshot = guideSnapshotRepository.findByPatchVersionForUpdate(patchVersion);
        validateSnapshotSource(patchVersion, targetSnapshot, resolvedSet);
        if (!completeImport) {
            validatePartialImportTarget(patchVersion, targetSnapshot);
        }

        ImportCounter counter = new ImportCounter();
        for (GuideCandidate candidate : candidates) {
            counter.add(upsertSplitGuide(candidate));
        }
        if (completeImport) {
            activateCompleteSnapshot(patchVersion, resolvedSet, counts, targetSnapshot);
        } else {
            stagePartialSnapshot(request, patchVersion, resolvedSet, counts, targetSnapshot);
        }

        return GuideImportResponse.builder()
                .patchVersion(patchVersion)
                .setNumber(resolvedSet.setNumber())
                .mutator(resolvedSet.mutator())
                .createdCount(counter.createdCount)
                .updatedCount(counter.updatedCount)
                .skippedCount(counter.skippedCount)
                .championCount(counts.championCount())
                .traitCount(counts.traitCount())
                .itemCount(counts.itemCount())
                .augmentCount(counts.augmentCount())
                .build();
    }

    private boolean isCompleteImport(GuideCdragonImportRequest request) {
        return request.shouldIncludeChampions()
                && request.shouldIncludeTraits()
                && request.shouldIncludeItems()
                && request.shouldIncludeAugments();
    }

    private GuideImportSource resolveRequestedSource(Integer setNumber, String mutator) {
        if (setNumber == null
                || setNumber < 1
                || !hasText(mutator)
                || mutator.trim().length() > MUTATOR_MAX_LENGTH) {
            logger.warn(
                    "CDragon guide import rejected because set number and mutator must be explicit. setNumber={}",
                    setNumber
            );
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return new GuideImportSource(setNumber, mutator.trim());
    }

    private void validateConfiguredPatchSource(
            String requestedPatchVersion,
            String resolvedPatchVersion,
            GuideImportSource requestedSource
    ) {
        Integer configuredSetNumber = guideCdragonImportProperties.getSetNumber();
        String configuredMutator = guideCdragonImportProperties.getMutator();
        boolean hasConfiguredSource = configuredSetNumber != null || configuredMutator != null;
        if (!guideCdragonImportProperties.isEnabled() && !hasConfiguredSource) {
            return;
        }

        GuideImportSource configuredSource = resolveRequestedSource(configuredSetNumber, configuredMutator);
        String configuredPatchVersion = resolveConfiguredPatchVersion(
                requestedPatchVersion,
                resolvedPatchVersion
        );
        if (comparePatchVersions(resolvedPatchVersion, configuredPatchVersion) > 0) {
            logger.warn(
                    "CDragon guide import rejected because the requested patch is newer than the configured "
                            + "production patch. requestedPatchVersion={}, configuredPatchVersion={}",
                    resolvedPatchVersion,
                    configuredPatchVersion
            );
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (configuredSource.equals(requestedSource)) {
            return;
        }
        if (!resolvedPatchVersion.equals(configuredPatchVersion)) {
            return;
        }

        logger.warn(
                "CDragon guide import rejected by configured patch-source mapping. "
                        + "patchVersion={}, requestedSetNumber={}, requestedMutator={}, "
                        + "configuredSetNumber={}, configuredMutator={}",
                resolvedPatchVersion,
                requestedSource.setNumber(),
                requestedSource.mutator(),
                configuredSource.setNumber(),
                configuredSource.mutator()
        );
        throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    private String resolveConfiguredPatchVersion(
            String requestedPatchVersion,
            String resolvedPatchVersion
    ) {
        String configuredPatchVersion = guideCdragonImportProperties.getPatchVersion();
        if (isLatestPatchVersion(configuredPatchVersion)
                && isLatestPatchVersion(requestedPatchVersion)) {
            return resolvedPatchVersion;
        }
        return normalizePatchVersion(configuredPatchVersion);
    }

    private boolean isLatestPatchVersion(String patchVersion) {
        return hasText(patchVersion)
                && LATEST_PATCH_VERSION_ALIAS.equalsIgnoreCase(patchVersion.trim());
    }

    private void validateSnapshotSource(
            String patchVersion,
            Optional<GuideSnapshot> targetSnapshot,
            ResolvedCdragonSet resolvedSet
    ) {
        if (targetSnapshot.isEmpty()
                || !targetSnapshot.get().hasSource()
                || targetSnapshot.get().matchesSource(resolvedSet.setNumber(), resolvedSet.mutator())) {
            return;
        }

        GuideSnapshot snapshot = targetSnapshot.get();
        logger.warn(
                "CDragon guide import rejected because snapshot source differs. "
                        + "patchVersion={}, requestedSetNumber={}, requestedMutator={}, "
                        + "snapshotSetNumber={}, snapshotMutator={}",
                patchVersion,
                resolvedSet.setNumber(),
                resolvedSet.mutator(),
                snapshot.getSourceSetNumber(),
                snapshot.getSourceMutator()
        );
        throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    private void validateRequestedImport(GuideCdragonImportRequest request, GuideImportCounts counts) {
        if ((request.shouldIncludeChampions()
                && counts.championCount() < guideCdragonImportProperties.getMinimumChampionCount())
                || (request.shouldIncludeTraits()
                && counts.traitCount() < guideCdragonImportProperties.getMinimumTraitCount())
                || (request.shouldIncludeItems()
                && counts.itemCount() < guideCdragonImportProperties.getMinimumItemCount())
                || (request.shouldIncludeAugments()
                && counts.augmentCount() < guideCdragonImportProperties.getMinimumAugmentCount())) {
            logger.warn(
                    "CDragon guide import rejected before persistence. champions={}, traits={}, items={}, augments={}",
                    counts.championCount(),
                    counts.traitCount(),
                    counts.itemCount(),
                    counts.augmentCount()
            );
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validatePartialImportTarget(
            String patchVersion,
            Optional<GuideSnapshot> targetSnapshot
    ) {
        if (targetSnapshot.isPresent()
                && targetSnapshot.get().getStatus() != GuideSnapshotStatus.STAGING) {
            logger.warn(
                    "Partial CDragon guide import rejected for a public or historical snapshot. patchVersion={}, status={}",
                    patchVersion,
                    targetSnapshot.get().getStatus()
            );
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void activateCompleteSnapshot(
            String patchVersion,
            ResolvedCdragonSet resolvedSet,
            GuideImportCounts counts,
            Optional<GuideSnapshot> targetSnapshot
    ) {
        LocalDateTime validatedAt = LocalDateTime.now();
        GuideSnapshot snapshot = targetSnapshot
                .orElseGet(() -> GuideSnapshot.builder()
                        .patchVersion(patchVersion)
                        .sourceSetNumber(resolvedSet.setNumber())
                        .sourceMutator(resolvedSet.mutator())
                        .status(GuideSnapshotStatus.STAGING)
                        .championCount(counts.championCount())
                        .traitCount(counts.traitCount())
                        .itemCount(counts.itemCount())
                        .augmentCount(counts.augmentCount())
                        .build());
        snapshot.recordSource(resolvedSet.setNumber(), resolvedSet.mutator());
        snapshot.updateValidation(
                counts.championCount(),
                counts.traitCount(),
                counts.itemCount(),
                counts.augmentCount(),
                validatedAt
        );
        Optional<GuideSnapshot> activeSnapshot = guideSnapshotRepository
                .findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE);
        boolean targetIsAlreadyActive = activeSnapshot
                .map(current -> current.getPatchVersion().equals(patchVersion))
                .orElse(false);

        if (targetIsAlreadyActive) {
            guideSnapshotRepository.save(snapshot);
            return;
        }

        boolean shouldActivate = activeSnapshot.isEmpty()
                || comparePatchVersions(patchVersion, activeSnapshot.get().getPatchVersion()) > 0;
        if (shouldActivate) {
            activeSnapshot.ifPresent(GuideSnapshot::deactivate);
            if (activeSnapshot.isPresent()) {
                guideSnapshotRepository.flush();
            }
            snapshot.activate(validatedAt);
        } else {
            snapshot.deactivate();
        }
        guideSnapshotRepository.save(snapshot);
    }

    private void stagePartialSnapshot(
            GuideCdragonImportRequest request,
            String patchVersion,
            ResolvedCdragonSet resolvedSet,
            GuideImportCounts counts,
            Optional<GuideSnapshot> targetSnapshot
    ) {
        if (targetSnapshot.isPresent()) {
            GuideSnapshot snapshot = targetSnapshot.get();
            snapshot.recordSource(resolvedSet.setNumber(), resolvedSet.mutator());
            snapshot.updateCounts(
                    request.shouldIncludeChampions() ? counts.championCount() : snapshot.getChampionCount(),
                    request.shouldIncludeTraits() ? counts.traitCount() : snapshot.getTraitCount(),
                    request.shouldIncludeItems() ? counts.itemCount() : snapshot.getItemCount(),
                    request.shouldIncludeAugments() ? counts.augmentCount() : snapshot.getAugmentCount()
            );
            guideSnapshotRepository.save(snapshot);
            return;
        }
        guideSnapshotRepository.save(GuideSnapshot.builder()
                .patchVersion(patchVersion)
                .sourceSetNumber(resolvedSet.setNumber())
                .sourceMutator(resolvedSet.mutator())
                .status(GuideSnapshotStatus.STAGING)
                .championCount(counts.championCount())
                .traitCount(counts.traitCount())
                .itemCount(counts.itemCount())
                .augmentCount(counts.augmentCount())
                .build());
    }

    private int comparePatchVersions(String left, String right) {
        PatchVersionParts leftParts = parsePatchVersion(left);
        PatchVersionParts rightParts = parsePatchVersion(right);

        int majorResult = Integer.compare(leftParts.major(), rightParts.major());
        if (majorResult != 0) {
            return majorResult;
        }
        int minorResult = Integer.compare(leftParts.minor(), rightParts.minor());
        if (minorResult != 0) {
            return minorResult;
        }
        int suffixResult = leftParts.suffix().compareToIgnoreCase(rightParts.suffix());
        if (suffixResult != 0) {
            return suffixResult;
        }
        return left.compareTo(right);
    }

    private PatchVersionParts parsePatchVersion(String patchVersion) {
        Matcher matcher = PATCH_VERSION_PATTERN.matcher(patchVersion == null ? "" : patchVersion.trim());
        if (!matcher.matches()) {
            return new PatchVersionParts(0, 0, patchVersion == null ? "" : patchVersion);
        }
        return new PatchVersionParts(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                matcher.group(3)
        );
    }

    private JsonNode fetchCdragonData() {
        try {
            String response = restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class);
            if (response == null || response.isBlank()) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
            }
            return objectMapper.readTree(response);
        } catch (RestClientException | JsonProcessingException e) {
            logger.error("Failed to fetch or parse Community Dragon TFT data. error={}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    private ResolvedCdragonSet resolveSetData(JsonNode root, int setNumber, String mutator) {
        return new ResolvedCdragonSet(
                findExplicitSetData(root, setNumber, mutator),
                setNumber,
                mutator
        );
    }

    private JsonNode findExplicitSetData(JsonNode root, int setNumber, String mutator) {
        for (JsonNode setData : root.path("setData")) {
            if (setData.path("number").asInt() == setNumber
                    && mutator.equals(setData.path("mutator").asText())) {
                return setData;
            }
        }

        JsonNode fallback = root.path("sets").path(String.valueOf(setNumber));
        if (("TFTSet" + setNumber).equals(mutator)
                && !fallback.isMissingNode()
                && fallback.has("champions")
                && fallback.has("traits")) {
            return fallback;
        }

        throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    private List<JsonNode> readSetChampions(JsonNode setData) {
        List<JsonNode> champions = new ArrayList<>();
        for (JsonNode champion : setData.path("champions")) {
            champions.add(champion);
        }
        return champions;
    }

    private List<JsonNode> readShopChampions(List<JsonNode> setChampions, int setNumber) {
        String championPrefix = "TFT" + setNumber + "_";
        List<JsonNode> champions = new ArrayList<>();
        for (JsonNode champion : setChampions) {
            String apiName = champion.path("apiName").asText();
            int cost = champion.path("cost").asInt(0);
            if (apiName.startsWith(championPrefix)
                    && !isSpecialUnit(apiName)
                    && cost >= 1
                    && cost <= 5
                    && champion.path("traits").isArray()
                    && champion.path("traits").size() > 0
                    && hasText(champion.path("name").asText())) {
                champions.add(champion);
            }
        }
        champions.sort(Comparator
                .comparing((JsonNode champion) -> champion.path("name").asText())
                .thenComparingInt(champion -> champion.path("cost").asInt()));
        return champions;
    }

    private List<GuideCandidate> toChampionCandidates(List<JsonNode> champions, String patchVersion) {
        List<GuideCandidate> candidates = new ArrayList<>();
        int sortOrder = 0;
        for (JsonNode champion : champions) {
            ObjectNode dataJson = objectMapper.createObjectNode();
            dataJson.put("cost", champion.path("cost").asInt());
            dataJson.put("role", normalizeRole(champion.path("role").asText()));
            dataJson.put("position", champion.path("stats").path("range").asInt() >= 3 ? "후방" : "전방");
            dataJson.set("traits", toTextArray(champion.path("traits")));
            dataJson.set("bestItems", objectMapper.createArrayNode());
            dataJson.set("stats", toChampionStats(champion.path("stats")));
            dataJson.set("ability", toAbility(champion.path("ability")));

            String summary = buildChampionSummary(champion.path("ability"));
            candidates.add(new GuideCandidate(
                    GuideType.CHAMPION,
                    champion.path("apiName").asText(),
                    champion.path("name").asText(),
                    summary,
                    championImageUrl(champion.path("apiName").asText(), champion),
                    dataJson,
                    patchVersion,
                    sortOrder++
            ));
        }
        return candidates;
    }

    private List<GuideCandidate> toTraitCandidates(
            JsonNode traits,
            List<JsonNode> champions,
            List<JsonNode> allChampions,
            String patchVersion
    ) {
        List<GuideCandidate> candidates = new ArrayList<>();
        int sortOrder = 0;
        boolean hasStargazerVariants = hasStargazerVariantTraits(traits);
        for (JsonNode trait : traits) {
            String apiName = trait.path("apiName").asText();
            String name = trait.path("name").asText();
            if (!hasText(apiName) || !hasText(name)) {
                continue;
            }
            if (hasStargazerVariants && isBaseStargazerTrait(apiName)) {
                continue;
            }

            ObjectNode dataJson = objectMapper.createObjectNode();
            dataJson.put("count", maxTraitCount(trait.path("effects")));
            dataJson.put("type", "시너지");
            String variant = stargazerVariant(apiName, trait.path("desc").asText(), trait.path("effects"));
            String summary = sanitizeTraitText(trait.path("desc").asText(), trait.path("effects"));
            if (hasText(variant)) {
                dataJson.put("variant", variant);
                summary = removeStargazerVariantIntro(summary, variant);
            }
            ArrayNode championRefs = traitChampionRefs(name, champions);
            if (championRefs.size() == 0) {
                logger.debug(
                        "CDragon trait skipped because no shop champion references it. apiName={}, name={}",
                        apiName,
                        name
                );
                continue;
            }

            dataJson.put("summary", summary);
            dataJson.put("tone", traitTone(trait.path("effects")));
            dataJson.set("levels", traitLevels(trait.path("effects")));
            dataJson.set("tierEffects", traitTierEffects(trait.path("desc").asText(), trait.path("effects")));
            dataJson.set("tips", objectMapper.createArrayNode());
            dataJson.set("champions", championRefs);
            dataJson.set("specialUnits", traitSpecialUnitRefs(apiName, allChampions));

            candidates.add(new GuideCandidate(
                    GuideType.TRAIT,
                    apiName,
                    name,
                    summary,
                    assetUrl(trait.path("icon").asText()),
                    dataJson,
                    patchVersion,
                    sortOrder++
            ));
        }
        return candidates;
    }

    private List<GuideCandidate> toItemCandidates(JsonNode items, String patchVersion) {
        Map<String, JsonNode> itemByApiName = mapItemsByApiName(items);
        List<JsonNode> completedItems = new ArrayList<>();
        for (JsonNode item : items) {
            if (isCompletedItem(item)) {
                completedItems.add(item);
            }
        }
        completedItems.sort(Comparator
                .comparing((JsonNode item) -> item.path("name").asText())
                .thenComparing(item -> item.path("apiName").asText()));

        List<GuideCandidate> candidates = new ArrayList<>();
        Set<String> importedItemNames = new HashSet<>();
        int sortOrder = 0;
        for (JsonNode item : completedItems) {
            String itemName = sanitizeDisplayName(item.path("name").asText());
            if (!hasText(itemName)) {
                continue;
            }
            if (!importedItemNames.add(normalizeMetricKey(itemName))) {
                logger.debug("Duplicate CDragon item skipped. apiName={}, name={}",
                        item.path("apiName").asText(),
                        itemName);
                continue;
            }
            String description = sanitizeCdragonText(item.path("desc").asText(), item.path("effects"));

            ObjectNode dataJson = objectMapper.createObjectNode();
            dataJson.set("bestUsers", objectMapper.createArrayNode());
            dataJson.put("category", "완성 아이템");
            dataJson.set("combinations", itemCombinations(item, itemByApiName));
            dataJson.put("description", description);

            candidates.add(new GuideCandidate(
                    GuideType.ITEM,
                    item.path("apiName").asText(),
                    itemName,
                    hasText(description) ? description : "CDragon 완성 아이템",
                    assetUrl(item.path("icon").asText()),
                    dataJson,
                    patchVersion,
                    sortOrder++
            ));
        }
        return candidates;
    }

    private Map<String, JsonNode> mapItemsByApiName(JsonNode items) {
        Map<String, JsonNode> itemByApiName = new HashMap<>();
        for (JsonNode item : items) {
            String apiName = item.path("apiName").asText();
            if (hasText(apiName)) {
                itemByApiName.put(apiName, item);
            }
        }
        return itemByApiName;
    }

    private boolean isCompletedItem(JsonNode item) {
        String apiName = item.path("apiName").asText();
        String name = item.path("name").asText();
        String description = sanitizeCdragonText(item.path("desc").asText(), item.path("effects"));
        if (!apiName.startsWith("TFT_Item_")
                || isUnsupportedItemVariant(apiName)
                || !hasResolvedCdragonText(name)
                || !hasResolvedCdragonText(description)
                || !hasText(item.path("icon").asText())) {
            return false;
        }
        if (item.path("associatedTraits").isArray() && item.path("associatedTraits").size() > 0) {
            return false;
        }
        return hasTwoCompositionItems(item.path("composition"));
    }

    private boolean isUnsupportedItemVariant(String apiName) {
        String normalizedApiName = apiName.toLowerCase(Locale.ROOT);
        return containsAny(
                normalizedApiName,
                "corrupted",
                "cursed",
                "shadow",
                "radiant",
                "ornn",
                "artifact",
                "support"
        );
    }

    private boolean hasTwoCompositionItems(JsonNode composition) {
        if (!composition.isArray() || composition.size() != 2) {
            return false;
        }
        for (JsonNode component : composition) {
            if (!hasText(component.asText())) {
                return false;
            }
        }
        return true;
    }

    private ArrayNode itemCombinations(JsonNode item, Map<String, JsonNode> itemByApiName) {
        ArrayNode componentRefs = objectMapper.createArrayNode();
        for (JsonNode componentKey : item.path("composition")) {
            componentRefs.add(itemRef(componentKey.asText(), itemByApiName));
        }

        ArrayNode combinations = objectMapper.createArrayNode();
        ObjectNode combination = objectMapper.createObjectNode();
        combination.set("items", componentRefs);
        combination.put("label", "조합식");
        combination.put("note", "재료 2개 조합");
        combinations.add(combination);
        return combinations;
    }

    private ObjectNode itemRef(String apiName, Map<String, JsonNode> itemByApiName) {
        JsonNode item = itemByApiName.get(apiName);
        ObjectNode ref = objectMapper.createObjectNode();
        ref.put("imageUrl", item == null ? "" : assetUrlOrEmpty(item.path("icon").asText()));
        ref.put("name", item == null ? apiName : item.path("name").asText(apiName));
        return ref;
    }

    private List<JsonNode> readAugments(JsonNode augmentRefs, JsonNode items) {
        Map<String, JsonNode> itemByApiName = mapItemsByApiName(items);
        List<JsonNode> augments = new ArrayList<>();
        for (JsonNode augmentRef : augmentRefs) {
            if (augmentRef.isTextual()) {
                JsonNode augment = itemByApiName.get(augmentRef.asText());
                if (augment != null) {
                    augments.add(augment);
                }
                continue;
            }
            if (augmentRef.isObject()) {
                augments.add(augmentRef);
            }
        }
        return augments;
    }

    private List<GuideCandidate> toAugmentCandidates(List<JsonNode> augments, String patchVersion) {
        List<JsonNode> importableAugments = new ArrayList<>();
        for (JsonNode augment : augments) {
            if (isImportableAugment(augment)) {
                importableAugments.add(augment);
            }
        }
        importableAugments.sort(Comparator.comparing(augment -> sanitizeDisplayName(augment.path("name").asText())));

        List<GuideCandidate> candidates = new ArrayList<>();
        Set<String> importedAugmentNames = new HashSet<>();
        int sortOrder = 0;
        for (JsonNode augment : importableAugments) {
            String augmentName = sanitizeDisplayName(augment.path("name").asText());
            if (!hasText(augmentName)) {
                continue;
            }
            if (!importedAugmentNames.add(normalizeMetricKey(augmentName))) {
                logger.debug("Duplicate CDragon augment skipped. apiName={}, name={}",
                        augment.path("apiName").asText(),
                        augmentName);
                continue;
            }
            String description = sanitizeCdragonText(readText(augment, "desc", "description", "tooltip"), augment.path("effects"));
            ObjectNode dataJson = objectMapper.createObjectNode();
            dataJson.put("description", description);
            dataJson.set("tags", augmentTags(augment, description));
            candidates.add(new GuideCandidate(
                    GuideType.AUGMENT,
                    augment.path("apiName").asText(),
                    augmentName,
                    hasText(description) ? description : "CDragon 증강체",
                    assetUrl(augment.path("icon").asText()),
                    dataJson,
                    patchVersion,
                    sortOrder++
            ));
        }
        return candidates;
    }

    private boolean isImportableAugment(JsonNode augment) {
        String apiName = augment.path("apiName").asText();
        String rawName = augment.path("name").asText();
        String name = sanitizeDisplayName(rawName);
        String description = sanitizeCdragonText(readText(augment, "desc", "description", "tooltip"), augment.path("effects"));
        if (!apiName.startsWith("TFT")
                || !hasResolvedCdragonText(rawName)
                || !hasText(name)
                || !hasResolvedCdragonText(description)
                || !hasText(augment.path("icon").asText())) {
            return false;
        }

        String searchable = (apiName + " " + name + " " + description).toLowerCase(Locale.ROOT);
        return !containsAny(searchable, "debug", "dummy", "test", "placeholder", "inactive", "disabled");
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String readText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText();
            if (hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private ArrayNode augmentTags(JsonNode augment, String description) {
        ArrayNode tags = objectMapper.createArrayNode();
        for (JsonNode cdragonTag : augment.path("tags")) {
            addDisplayTag(tags, sanitizeDisplayName(cdragonTag.asText()));
        }
        addDerivedAugmentTags(tags, description);
        if (tags.isEmpty()) {
            tags.add("공용");
        }
        return tags;
    }

    private void addDerivedAugmentTags(ArrayNode tags, String description) {
        String searchable = description.toLowerCase(Locale.ROOT);
        if (containsAny(searchable, AUGMENT_REROLL_KEYWORDS)) {
            addDisplayTag(tags, "리롤");
        }
        if (containsAny(searchable, AUGMENT_ECONOMY_KEYWORDS)) {
            addDisplayTag(tags, "경제");
        }
        if (containsAny(searchable, AUGMENT_ITEM_TAG_KEYWORDS)) {
            addDisplayTag(tags, "아이템");
        }
        if (containsAny(searchable, AUGMENT_CHAMPION_KEYWORDS)) {
            addDisplayTag(tags, "챔피언");
        }
        if (containsAny(searchable, AUGMENT_SYNERGY_KEYWORDS)) {
            addDisplayTag(tags, "시너지");
        }
        if (containsAny(searchable, AUGMENT_QUEST_KEYWORDS)) {
            addDisplayTag(tags, "퀘스트");
        }
        if (containsAny(searchable, AUGMENT_COMBAT_TAG_KEYWORDS)) {
            addDisplayTag(tags, "전투");
        }
    }

    private void addDisplayTag(ArrayNode tags, String tag) {
        if (tags.size() >= AUGMENT_TAG_LIMIT || !isDisplayableAugmentTag(tag) || containsTag(tags, tag)) {
            return;
        }
        tags.add(tag);
    }

    private boolean isDisplayableAugmentTag(String tag) {
        return hasText(tag)
                && !HASH_TAG_PATTERN.matcher(tag.trim()).matches()
                && !CDRAGON_TOKEN_PATTERN.matcher(tag.trim()).matches();
    }

    private boolean containsTag(ArrayNode tags, String tag) {
        for (JsonNode existingTag : tags) {
            if (existingTag.asText().equals(tag)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeMetricKey(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private ObjectNode toChampionStats(JsonNode stats) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("ad", stats.path("damage").asInt());
        data.put("armor", stats.path("armor").asInt());
        data.put("attackSpeed", String.format(Locale.ROOT, "%.2f", stats.path("attackSpeed").asDouble()));
        data.put("hp", stats.path("hp").asInt());
        data.put("mana", stats.path("initialMana").asInt() + "/" + stats.path("mana").asInt());
        data.put("mr", stats.path("magicResist").asInt());
        data.put("range", stats.path("range").asInt());
        return data;
    }

    private ObjectNode toAbility(JsonNode ability) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("name", ability.path("name").asText());
        data.put("description", sanitizeAbilityDescription(ability));
        data.put("iconUrl", assetUrl(ability.path("icon").asText()));
        return data;
    }

    private ArrayNode traitChampionRefs(String traitName, List<JsonNode> champions) {
        ArrayNode refs = objectMapper.createArrayNode();
        for (JsonNode champion : champions) {
            if (!containsText(champion.path("traits"), traitName)) {
                continue;
            }
            ObjectNode ref = objectMapper.createObjectNode();
            ref.put("cost", champion.path("cost").asInt());
            ref.put("imageUrl", championImageUrl(champion.path("apiName").asText(), champion));
            ref.put("name", champion.path("name").asText());
            refs.add(ref);
        }
        return refs;
    }

    private ArrayNode traitSpecialUnitRefs(String traitApiName, List<JsonNode> champions) {
        ArrayNode refs = objectMapper.createArrayNode();
        Set<String> specialUnitKeys = TRAIT_SPECIAL_UNIT_KEYS.getOrDefault(traitApiName, Set.of());
        if (specialUnitKeys.isEmpty()) {
            return refs;
        }

        for (JsonNode champion : champions) {
            String apiName = champion.path("apiName").asText();
            if (!specialUnitKeys.contains(apiName)) {
                continue;
            }

            ObjectNode ref = objectMapper.createObjectNode();
            ref.put("imageUrl", championImageUrl(apiName, champion));
            ref.put("name", champion.path("name").asText());
            ref.put("note", "특성 효과로 생성");
            refs.add(ref);
        }
        return refs;
    }

    private ArrayNode traitLevels(JsonNode effects) {
        ArrayNode levels = objectMapper.createArrayNode();
        for (JsonNode effect : effects) {
            String level = traitLevel(effect);
            if (!hasText(level)) {
                continue;
            }
            levels.add(level);
        }
        return levels;
    }

    private ArrayNode traitTierEffects(String value, JsonNode effects) {
        ArrayNode tierEffects = objectMapper.createArrayNode();
        if (!hasText(value)) {
            return tierEffects;
        }

        Matcher rowMatcher = ROW_TAG_PATTERN.matcher(value);
        int rowIndex = 0;
        while (rowMatcher.find()) {
            JsonNode effect = effectAt(effects, rowIndex++);
            String level = traitLevel(effect);
            if (!hasText(level)) {
                continue;
            }

            String description = traitTierEffectDescription(rowMatcher.group(2), effect);
            if (!hasText(description)) {
                continue;
            }

            ObjectNode tierEffect = objectMapper.createObjectNode();
            tierEffect.put("level", level);
            tierEffect.put("description", description);
            tierEffects.add(tierEffect);
        }
        return tierEffects;
    }

    private String traitTierEffectDescription(String rowText, JsonNode effect) {
        String metricLabel = inferTraitMetricLabel(rowText);
        String withIconLabels = replaceCdragonIconTokens(rowText);
        String description = stripTraitLevelPrefix(sanitizeText(interpolatePlaceholders(withIconLabels, effect)));
        return normalizeTraitMetricDescription(description, metricLabel);
    }

    private String replaceCdragonIconTokens(String value) {
        Matcher tokenMatcher = CDRAGON_TOKEN_PATTERN.matcher(value);
        StringBuffer replaced = new StringBuffer();
        while (tokenMatcher.find()) {
            String label = cdragonIconLabel(tokenMatcher.group());
            tokenMatcher.appendReplacement(replaced, Matcher.quoteReplacement(label));
        }
        tokenMatcher.appendTail(replaced);
        return replaced.toString();
    }

    private String cdragonIconLabel(String token) {
        if (!hasText(token)) {
            return "";
        }
        String normalized = token
                .replace("%", "")
                .replaceFirst("(?i)^i:", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
        return CDRAGON_ICON_LABELS.getOrDefault(normalized, "");
    }

    private String inferTraitMetricLabel(String rowText) {
        if (!hasText(rowText)) {
            return "";
        }

        Matcher iconMatcher = CDRAGON_TOKEN_PATTERN.matcher(rowText);
        while (iconMatcher.find()) {
            String label = cdragonIconLabel(iconMatcher.group());
            if (hasText(label)) {
                return label;
            }
        }

        Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(rowText);
        while (placeholderMatcher.find()) {
            String label = metricLabelFromExpression(placeholderMatcher.group(1));
            if (hasText(label)) {
                return label;
            }
        }
        return "";
    }

    private String metricLabelFromExpression(String expression) {
        if (!hasText(expression)) {
            return "";
        }
        String normalized = MULTIPLY_EXPRESSION_PATTERN.matcher(expression.trim())
                .replaceFirst("$1")
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
        if (normalized.contains("attackspeed") || normalized.equals("as")) {
            return "공격 속도";
        }
        if (normalized.contains("abilitypower") || normalized.equals("ap")) {
            return "주문력";
        }
        if (normalized.contains("attackdamage") || normalized.endsWith("ad")) {
            return "공격력";
        }
        if (normalized.contains("health") || normalized.contains("hp")) {
            return "체력";
        }
        if (normalized.contains("magicresist") || normalized.endsWith("mr")) {
            return "마법 저항력";
        }
        if (normalized.contains("armor")) {
            return "방어력";
        }
        if (normalized.contains("mana")) {
            return "마나";
        }
        if (normalized.contains("crit")) {
            return "치명타";
        }
        if (normalized.contains("damage")) {
            return "피해";
        }
        if (normalized.contains("gold")) {
            return "골드";
        }
        return "";
    }

    private String normalizeTraitMetricDescription(String description, String metricLabel) {
        if (!hasText(description) || !hasText(metricLabel)) {
            return description;
        }
        if (description.startsWith(metricLabel)) {
            return description;
        }
        if (METRIC_ONLY_PATTERN.matcher(description).matches()) {
            return metricLabel + " " + description;
        }
        if (description.endsWith(" " + metricLabel)) {
            return metricLabel + " " + description.substring(0, description.length() - metricLabel.length()).trim();
        }
        return description;
    }

    private String traitLevel(JsonNode effect) {
        int minUnits = effect.path("minUnits").asInt();
        int maxUnits = effect.path("maxUnits").asInt();
        if (minUnits <= 0) {
            return "";
        }
        return maxUnits >= 25000 ? minUnits + "+" : String.valueOf(minUnits);
    }

    private String stripTraitLevelPrefix(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value
                .replaceFirst("^\\s*\\([^)]*\\)\\s*", "")
                .replaceFirst("^\\s*\\d+\\+?\\s+", "")
                .trim();
    }

    private String removeTraitRows(String value) {
        if (!hasText(value)) {
            return "";
        }
        return ROW_TAG_PATTERN.matcher(value).replaceAll(" ");
    }

    private boolean isBaseStargazerTrait(String apiName) {
        return hasText(apiName) && apiName.matches("TFT\\d+_Stargazer");
    }

    private boolean hasStargazerVariantTraits(JsonNode traits) {
        for (JsonNode trait : traits) {
            if (isStargazerVariantTrait(trait.path("apiName").asText())) {
                return true;
            }
        }
        return false;
    }

    private String stargazerVariant(String apiName, String value, JsonNode effects) {
        if (!isStargazerVariantTrait(apiName) || !hasText(value)) {
            return "";
        }
        String summaryOnly = prepareTraitSummaryText(value);
        String interpolated = interpolatePlaceholders(summaryOnly, firstEffect(effects));
        Matcher matcher = STARGAZER_VARIANT_PATTERN.matcher(sanitizeText(interpolated));
        return matcher.find() ? matcher.group(1).replaceFirst("[.:。]+$", "").trim() : "";
    }

    private boolean isStargazerVariantTrait(String apiName) {
        return hasText(apiName) && apiName.matches("TFT\\d+_Stargazer_.+");
    }

    private String removeStargazerVariantIntro(String summary, String variant) {
        if (!hasText(summary) || !hasText(variant)) {
            return summary;
        }
        String normalized = summary;
        String commonIntro = "별돌보미는 게임마다 다른 별자리를 그립니다.";
        if (normalized.startsWith(commonIntro)) {
            normalized = normalized.substring(commonIntro.length()).trim();
        }
        String variantIntro = "이번 게임: " + variant;
        if (normalized.startsWith(variantIntro)) {
            normalized = normalized.substring(variantIntro.length()).trim();
            normalized = normalized.replaceFirst("^\\([^)]*\\)\\s*", "").trim();
            normalized = normalized.replaceFirst("^[\\s.]+", "").trim();
        }
        return normalized;
    }

    private int maxTraitCount(JsonNode effects) {
        int max = 0;
        for (JsonNode effect : effects) {
            int minUnits = effect.path("minUnits").asInt();
            max = Math.max(max, minUnits);
        }
        return max;
    }

    private String traitTone(JsonNode effects) {
        int maxStyle = 0;
        for (JsonNode effect : effects) {
            maxStyle = Math.max(maxStyle, effect.path("style").asInt());
        }
        if (maxStyle >= 4) {
            return "prismatic";
        }
        if (maxStyle == 3) {
            return "gold";
        }
        if (maxStyle == 2) {
            return "silver";
        }
        return "bronze";
    }

    private ArrayNode toTextArray(JsonNode values) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (JsonNode value : values) {
            if (hasText(value.asText())) {
                arrayNode.add(value.asText());
            }
        }
        return arrayNode;
    }

    private boolean containsText(JsonNode values, String expected) {
        for (JsonNode value : values) {
            if (expected.equals(value.asText())) {
                return true;
            }
        }
        return false;
    }

    private ImportResult upsertSplitGuide(GuideCandidate candidate) {
        return switch (candidate.guideType()) {
            case CHAMPION -> upsertGuideChampion(candidate);
            case TRAIT -> upsertGuideTrait(candidate);
            case ITEM -> upsertGuideItem(candidate);
            case AUGMENT -> upsertGuideAugment(candidate);
        };
    }

    private ImportResult upsertGuideChampion(GuideCandidate candidate) {
        JsonNode dataJson = candidate.dataJson();
        return guideChampionRepository.findByChampionKeyAndPatchVersion(candidate.targetKey(), candidate.patchVersion())
                .map(guideChampion -> {
                    guideChampion.update(
                            candidate.name(),
                            dataJson.path("cost").asInt(),
                            readText(dataJson, "role"),
                            readText(dataJson, "position"),
                            candidate.imageUrl(),
                            writeJsonField(dataJson, "stats", objectMapper.createObjectNode()),
                            writeJsonField(dataJson, "traits", objectMapper.createArrayNode()),
                            writeJsonField(dataJson, "bestItems", objectMapper.createArrayNode())
                    );
                    return ImportResult.UPDATED;
                })
                .orElseGet(() -> {
                    guideChampionRepository.save(GuideChampion.builder()
                            .championKey(candidate.targetKey())
                            .name(candidate.name())
                            .cost(dataJson.path("cost").asInt())
                            .role(readText(dataJson, "role"))
                            .position(readText(dataJson, "position"))
                            .imageUrl(candidate.imageUrl())
                            .statsJson(writeJsonField(dataJson, "stats", objectMapper.createObjectNode()))
                            .traitsJson(writeJsonField(dataJson, "traits", objectMapper.createArrayNode()))
                            .bestItemsJson(writeJsonField(dataJson, "bestItems", objectMapper.createArrayNode()))
                            .patchVersion(candidate.patchVersion())
                            .build());
                    return ImportResult.CREATED;
                });
    }

    private ImportResult upsertGuideTrait(GuideCandidate candidate) {
        JsonNode dataJson = candidate.dataJson();
        return guideTraitRepository.findByTraitKeyAndPatchVersion(candidate.targetKey(), candidate.patchVersion())
                .map(guideTrait -> {
                    guideTrait.update(
                            candidate.name(),
                            readText(dataJson, "type"),
                            candidate.imageUrl(),
                            readText(dataJson, "tone"),
                            candidate.summary(),
                            writeJsonField(dataJson, "levels", objectMapper.createArrayNode()),
                            writeJsonField(dataJson, "tierEffects", objectMapper.createArrayNode()),
                            writeJsonField(dataJson, "champions", objectMapper.createArrayNode()),
                            writeJsonField(dataJson, "specialUnits", objectMapper.createArrayNode()),
                            writeJsonField(dataJson, "tips", objectMapper.createArrayNode())
                    );
                    return ImportResult.UPDATED;
                })
                .orElseGet(() -> {
                    guideTraitRepository.save(GuideTrait.builder()
                            .traitKey(candidate.targetKey())
                            .name(candidate.name())
                            .type(readText(dataJson, "type"))
                            .iconUrl(candidate.imageUrl())
                            .tone(readText(dataJson, "tone"))
                            .summary(candidate.summary())
                            .levelsJson(writeJsonField(dataJson, "levels", objectMapper.createArrayNode()))
                            .tierEffectsJson(writeJsonField(dataJson, "tierEffects", objectMapper.createArrayNode()))
                            .championsJson(writeJsonField(dataJson, "champions", objectMapper.createArrayNode()))
                            .specialUnitsJson(writeJsonField(dataJson, "specialUnits", objectMapper.createArrayNode()))
                            .tipsJson(writeJsonField(dataJson, "tips", objectMapper.createArrayNode()))
                            .patchVersion(candidate.patchVersion())
                            .build());
                    return ImportResult.CREATED;
                });
    }

    private ImportResult upsertGuideItem(GuideCandidate candidate) {
        JsonNode dataJson = candidate.dataJson();
        return guideItemRepository.findByItemKeyAndPatchVersion(candidate.targetKey(), candidate.patchVersion())
                .map(guideItem -> {
                    guideItem.update(
                            candidate.name(),
                            readText(dataJson, "category"),
                            candidate.imageUrl(),
                            readText(dataJson, "description"),
                            writeJson(objectMapper.createObjectNode()),
                            writeJsonField(dataJson, "bestUsers", objectMapper.createArrayNode()),
                            writeJsonField(dataJson, "combinations", objectMapper.createArrayNode())
                    );
                    return ImportResult.UPDATED;
                })
                .orElseGet(() -> {
                    guideItemRepository.save(GuideItem.builder()
                            .itemKey(candidate.targetKey())
                            .name(candidate.name())
                            .category(readText(dataJson, "category"))
                            .imageUrl(candidate.imageUrl())
                            .description(readText(dataJson, "description"))
                            .statsJson(writeJson(objectMapper.createObjectNode()))
                            .bestUsersJson(writeJsonField(dataJson, "bestUsers", objectMapper.createArrayNode()))
                            .combinationsJson(writeJsonField(dataJson, "combinations", objectMapper.createArrayNode()))
                            .patchVersion(candidate.patchVersion())
                            .build());
                    return ImportResult.CREATED;
                });
    }

    private ImportResult upsertGuideAugment(GuideCandidate candidate) {
        JsonNode dataJson = candidate.dataJson();
        return guideAugmentRepository.findByAugmentKeyAndPatchVersion(candidate.targetKey(), candidate.patchVersion())
                .map(guideAugment -> {
                    guideAugment.update(
                            candidate.name(),
                            readText(dataJson, "description"),
                            candidate.imageUrl(),
                            writeJsonField(dataJson, "tags", objectMapper.createArrayNode()),
                            writeJson(objectMapper.createObjectNode())
                    );
                    return ImportResult.UPDATED;
                })
                .orElseGet(() -> {
                    guideAugmentRepository.save(GuideAugment.builder()
                            .augmentKey(candidate.targetKey())
                            .name(candidate.name())
                            .description(readText(dataJson, "description"))
                            .iconUrl(candidate.imageUrl())
                            .tagsJson(writeJsonField(dataJson, "tags", objectMapper.createArrayNode()))
                            .statsJson(writeJson(objectMapper.createObjectNode()))
                            .patchVersion(candidate.patchVersion())
                            .build());
                    return ImportResult.CREATED;
                });
    }

    private String writeJsonField(JsonNode dataJson, String fieldName, JsonNode fallbackValue) {
        JsonNode value = dataJson.path(fieldName);
        return writeJson(value.isMissingNode() || value.isNull() ? fallbackValue : value);
    }

    private String writeJson(JsonNode dataJson) {
        try {
            return objectMapper.writeValueAsString(dataJson);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize imported guide dataJson. error={}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.GUIDE_INVALID_DATA);
        }
    }

    private int countByType(List<GuideCandidate> candidates, GuideType guideType) {
        int count = 0;
        for (GuideCandidate candidate : candidates) {
            if (candidate.guideType() == guideType) {
                count++;
            }
        }
        return count;
    }

    private String assetUrl(String assetPath) {
        if (!hasText(assetPath)) {
            return null;
        }
        return communityDragonProperties.getAssetBaseUrl()
                + "/"
                + assetPath.toLowerCase(Locale.ROOT).replace(".tex", ".png");
    }

    private boolean isSpecialUnit(String apiName) {
        return SPECIAL_UNIT_KEYS.contains(apiName);
    }

    private String championImageUrl(String apiName, JsonNode champion) {
        String overridePath = championImageAssetPathOverride(apiName);
        if (overridePath != null) {
            return assetUrl(overridePath);
        }
        return assetUrl(champion.path("squareIcon").asText(champion.path("icon").asText()));
    }

    private String championImageAssetPathOverride(String apiName) {
        return switch (apiName) {
            case "TFT17_Rhaast" -> "ASSETS/Characters/TFT17_Rhaast/HUD/TFT17_Kayn_Slay_Square.TFT_Set17.tex";
            case "TFT17_DarkStar_FakeUnit" ->
                    "ASSETS/Characters/TFT17_DarkStar_FakeUnit/HUD/TFT17_DarkStar_FakeUnit_SmallSplash.TFT_Set17.tex";
            default -> null;
        };
    }

    private String assetUrlOrEmpty(String assetPath) {
        String url = assetUrl(assetPath);
        return url == null ? "" : url;
    }

    private String buildChampionSummary(JsonNode ability) {
        String abilityName = ability.path("name").asText();
        String description = sanitizeAbilityDescription(ability);
        if (!hasText(abilityName)) {
            return description;
        }
        if (!hasText(description)) {
            return abilityName;
        }
        return abilityName + " - " + description;
    }

    private String sanitizeAbilityDescription(JsonNode ability) {
        return sanitizeCdragonText(ability.path("desc").asText(), resolveAbilityEffectNode(ability));
    }

    private JsonNode resolveAbilityEffectNode(JsonNode ability) {
        JsonNode effects = ability.path("effects");
        if (effects.isArray()) {
            return firstEffect(effects);
        }
        if (effects.isObject() && !effects.isEmpty()) {
            return effects;
        }
        return ability;
    }

    private String sanitizeText(String value) {
        if (!hasText(value)) {
            return "";
        }
        String withoutBreakTags = BREAK_TAG_PATTERN.matcher(value).replaceAll(" ");
        String withoutTags = TAG_PATTERN.matcher(withoutBreakTags).replaceAll("");
        String withoutPlaceholders = PLACEHOLDER_PATTERN.matcher(withoutTags).replaceAll("");
        String withoutTemplateTokens = replaceDoubleBraceTokens(withoutPlaceholders);
        String withoutCdragonTokens = CDRAGON_TOKEN_PATTERN.matcher(withoutTemplateTokens).replaceAll("");
        String normalizedPercent = withoutCdragonTokens.replaceAll("(\\d)\\s+%", "$1%");
        String withoutEmptyParens = EMPTY_PARENS_PATTERN.matcher(normalizedPercent).replaceAll("");
        String normalizedPlaceholders = normalizeUnresolvedPlaceholderText(withoutEmptyParens);
        String cleaned = STANDALONE_PERCENT_PATTERN.matcher(normalizedPlaceholders)
                .replaceAll("$1")
                .replaceAll("\\s+([.,!?])", "$1")
                .replaceAll("\\s+", " ")
                .trim();
        String normalized = normalizeUnresolvedPlaceholderText(cleaned)
                .replaceAll("\\s+([.,!?])", "$1")
                .replaceAll("\\s+", " ")
                .trim();
        return normalizeGuideText(normalized);
    }

    private String normalizeUnresolvedPlaceholderText(String value) {
        String normalized = value.replaceAll("\\s+", " ");
        return normalized
                .replaceAll("\\s*\\([^)]*수치[^)]*\\)", "")
                .replaceAll("[:：]\\s*수치(?:\\s*,\\s*수치)*", "")
                .replaceAll("수치\\s+수치(?:\\s+수치)+", "")
                .replaceAll("사용 시회의", "사용 시 여러 회의")
                .replaceAll("보호막을\\s+수치,\\s+내구력을", "보호막과 내구력을")
                .replaceAll("체력을\\s+수치\\s+더 회복", "체력을 더 회복")
                .replaceAll("최대 체력을\\s+수치\\s+더 얻", "최대 체력을 더 얻")
                .replaceAll("피해의\\s+수치를\\s+저장", "피해량을 저장")
                .replaceAll("체력을\\s+수치\\s+흡수", "체력을 흡수")
                .replaceAll("추가 수치를", "추가 능력치를")
                .replaceAll("속도의 수치를", "속도의 일부를")
                .replaceAll("수치가\\s+증가", "효과가 증가")
                .replaceAll("수치\\s+회복", "회복")
                .replaceAll("수치\\s*일정 시간", "일정 시간")
                .replaceAll("수치(?=\\d)", "")
                .replaceAll("수치여러", "여러")
                .replaceAll("수치발", "여러 발")
                .replaceAll("수치\\s*의\\s+추가\\s+마법물리 피해", "추가 피해")
                .replaceAll("수치\\s*의\\s+마법물리 피해", "피해")
                .replaceAll("수치\\s*의\\s+추가\\s+마법 피해", "추가 마법 피해")
                .replaceAll("수치\\s*의\\s+추가\\s+물리 피해", "추가 물리 피해")
                .replaceAll("수치\\s*의\\s+추가\\s+고정 피해", "추가 고정 피해")
                .replaceAll("수치\\s*의\\s+보호막", "보호막")
                .replaceAll("수치\\s*의\\s+마법 피해", "마법 피해")
                .replaceAll("수치\\s*의\\s+물리 피해", "물리 피해")
                .replaceAll("수치\\s*의\\s+고정 피해", "고정 피해")
                .replaceAll("수치\\s*의\\s+피해", "피해")
                .replaceAll("수치\\s*의\\s+", "")
                .replaceAll("수치\\s*초", "일정 시간")
                .replaceAll("수치\\s*명", "여러 명")
                .replaceAll("수치\\s*개", "여러 개")
                .replaceAll("수치\\s*마리", "여러 마리")
                .replaceAll("수치\\s*회", "여러 회")
                .replaceAll("수치\\s*칸", "여러 칸")
                .replaceAll("수치\\s*표식", "표식")
                .replaceAll("수치당", "비율에 따라")
                .replaceAll("수치\\s+아래", "일정 기준 아래")
                .replaceAll("수치\\s+이상", "일정 기준 이상")
                .replaceAll("수치에\\s+해당하는", "일정량에 해당하는")
                .replaceAll("수치\\s+확률", "일정 확률")
                .replaceAll("수치\\s+냉각", "냉각")
                .replaceAll("수치씩\\s+감소", "점차 감소")
                .replaceAll("수치\\s+감소", "감소")
                .replaceAll("수치\\s+증가한", "증가한")
                .replaceAll("수치\\s+증가시", "증가 시")
                .replaceAll("수치\\s+둔화", "둔화")
                .replaceAll("수치\\s+무시", "일부 무시")
                .replaceAll("수치\\s+얻", "얻")
                .replaceAll("수치\\s+획득", "효과 획득")
                .replaceAll("수치\\s+더 회복", "더 회복")
                .replaceAll("수치\\s+더 얻", "더 얻")
                .replaceAll("수치\\s+추가로", "추가로")
                .replaceAll("수치\\s+행운 확률", "행운 확률")
                .replaceAll("수치\\s+정령", "정령")
                .replaceAll("수치~", "")
                .replaceAll("체력을\\s+수치\\s+얻", "체력을 얻")
                .replaceAll("최대 체력을\\s+영구적으로\\s+수치\\s+얻", "최대 체력을 영구적으로 얻")
                .replaceAll("공격 속도를\\s+수치\\s+얻", "공격 속도를 얻")
                .replaceAll("능력치를\\s+수치\\s+얻", "능력치를 얻")
                .replaceAll("방어력 및 마법 저항력을\\s+수치\\s+얻", "방어력 및 마법 저항력을 얻")
                .replaceAll("수치\\s+증가", "증가")
                .replaceAll("수치\\s*만큼", "일정량만큼")
                .replaceAll("수치의\\s+보호막", "보호막")
                .replaceAll("수치의\\s+마법 피해", "마법 피해")
                .replaceAll("수치의\\s+물리 피해", "물리 피해")
                .replaceAll("수치의\\s+고정 피해", "고정 피해")
                .replaceAll("수치의\\s+피해", "피해")
                .replaceAll("\\s+수치$", "");
    }

    private String replaceDoubleBraceTokens(String value) {
        Matcher tokenMatcher = DOUBLE_BRACE_TOKEN_PATTERN.matcher(value);
        StringBuffer replaced = new StringBuffer();
        while (tokenMatcher.find()) {
            String tokenName = tokenMatcher.group(1).trim();
            String replacement = CDRAGON_TEMPLATE_TOKEN_LABELS.getOrDefault(tokenName, "");
            tokenMatcher.appendReplacement(replaced, Matcher.quoteReplacement(replacement));
        }
        tokenMatcher.appendTail(replaced);
        return replaced.toString();
    }

    private String sanitizeCdragonText(String value, JsonNode effects) {
        if (!hasText(value)) {
            return "";
        }
        return sanitizeText(interpolatePlaceholders(value, effects));
    }

    private String sanitizeTraitText(String value, JsonNode effects) {
        if (!hasText(value)) {
            return "";
        }
        String summaryOnly = prepareTraitSummaryText(value);
        String interpolated = interpolatePlaceholders(summaryOnly, firstEffect(effects));
        return sanitizeText(interpolated);
    }

    private String prepareTraitSummaryText(String value) {
        String withoutRows = removeTraitRows(value);
        String withoutInactiveBlocks = SHOW_IF_NOT_BLOCK_PATTERN.matcher(withoutRows).replaceAll(" ");
        return BREAK_TAG_PATTERN.matcher(withoutInactiveBlocks).replaceAll(". ");
    }

    private String sanitizeDisplayName(String value) {
        return sanitizeText(value);
    }

    private String normalizeGuideText(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value
                .replaceAll("'\\s*\\.\\.\\.\\s*", "'")
                .replaceAll("\\s*\\.\\.\\.\\s*'", "'")
                .replaceAll("(?:\\s*\\.\\s*){2,}", ". ")
                .replaceAll("^[\\s.,:;]+", "")
                .replaceAll("\\s+([.,!?])", "$1")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String interpolatePlaceholders(String value, JsonNode effect) {
        Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer interpolated = new StringBuffer();
        while (placeholderMatcher.find()) {
            String expression = placeholderMatcher.group(1);
            String replacement = resolveEffectValue(expression, effect);
            if (!hasText(replacement) && !isOpenEndedMaxUnits(expression, effect)) {
                replacement = "수치";
            }
            placeholderMatcher.appendReplacement(interpolated, Matcher.quoteReplacement(replacement));
        }
        placeholderMatcher.appendTail(interpolated);
        return interpolated.toString();
    }

    private boolean isOpenEndedMaxUnits(String expression, JsonNode effect) {
        String normalized = expression == null ? "" : expression.trim();
        return "MaxUnits".equals(normalized)
                && effect != null
                && effect.path("maxUnits").asDouble() >= 25000;
    }

    private JsonNode firstEffect(JsonNode effects) {
        return effectAt(effects, 0);
    }

    private JsonNode effectAt(JsonNode effects, int index) {
        if (effects == null || !effects.isArray() || effects.isEmpty()) {
            return objectMapper.createObjectNode();
        }
        if (index >= 0 && index < effects.size()) {
            return effects.get(index);
        }
        return effects.get(effects.size() - 1);
    }

    private String resolveEffectValue(String expression, JsonNode effect) {
        String normalized = expression == null ? "" : expression.trim();
        if (!hasText(normalized) || effect == null || effect.isMissingNode()) {
            return "";
        }
        if ("MinUnits".equals(normalized)) {
            return formatTraitNumber(effect.path("minUnits").asDouble());
        }
        if ("MaxUnits".equals(normalized)) {
            double maxUnits = effect.path("maxUnits").asDouble();
            return maxUnits >= 25000 ? "" : formatTraitNumber(maxUnits);
        }

        Matcher multiplyMatcher = MULTIPLY_EXPRESSION_PATTERN.matcher(normalized);
        if (multiplyMatcher.matches()) {
            return resolveVariableValue(
                    effect,
                    multiplyMatcher.group(1),
                    Double.parseDouble(multiplyMatcher.group(2))
            );
        }
        return resolveVariableValue(effect, normalized, 1);
    }

    private String resolveVariableValue(JsonNode effect, String variableName, double multiplier) {
        JsonNode value = findFieldIgnoreCase(effect.path("variables"), variableName);
        if (value.isMissingNode()) {
            value = findFieldIgnoreCase(effect, variableName);
        }
        if (value.isMissingNode() || value.isNull() || !value.isNumber()) {
            return "";
        }
        return formatTraitNumber(value.asDouble() * multiplier);
    }

    private JsonNode findFieldIgnoreCase(JsonNode node, String fieldName) {
        if (node == null || !node.isObject() || !hasText(fieldName)) {
            return objectMapper.missingNode();
        }

        JsonNode exactValue = node.path(fieldName);
        if (!exactValue.isMissingNode()) {
            return exactValue;
        }

        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String currentFieldName = fieldNames.next();
            if (currentFieldName.equalsIgnoreCase(fieldName)) {
                return node.path(currentFieldName);
            }
        }
        return objectMapper.missingNode();
    }

    private String formatTraitNumber(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private String normalizeRole(String role) {
        if (!hasText(role)) {
            return "역할 미정";
        }
        return switch (role) {
            case "ADCarry" -> "AD 캐리";
            case "APCarry" -> "AP 캐리";
            case "ADCaster" -> "AD 캐스터";
            case "APCaster" -> "AP 캐스터";
            case "ADFighter" -> "AD 전사";
            case "APFighter", "HFighter" -> "AP 전사";
            case "ADReaper" -> "AD 암살자";
            case "APReaper" -> "AP 암살자";
            case "ADSpecialist" -> "AD 특수";
            case "APSpecialist" -> "AP 특수";
            case "ADTank" -> "AD 탱커";
            case "APTank" -> "AP 탱커";
            default -> role;
        };
    }

    private String normalizeRequired(String value) {
        if (!hasText(value)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return value.trim();
    }

    private String normalizePatchVersion(String value) {
        String normalized = normalizeRequired(value);
        if (LATEST_PATCH_VERSION_ALIAS.equalsIgnoreCase(normalized)) {
            normalized = resolveCurrentPatchVersion();
        }
        if (normalized.length() > PATCH_VERSION_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return normalized;
    }

    private String resolveCurrentPatchVersion() {
        return patchNoteRepository.findFirstByCurrentTrueAndDeletedAtIsNullOrderByPublishedAtDescIdDesc()
                .or(() -> patchNoteRepository.findFirstByDeletedAtIsNullOrderByPublishedAtDescIdDesc())
                .map(PatchNote::getVersion)
                .filter(this::hasText)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean hasResolvedCdragonText(String value) {
        if (!hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !normalized.startsWith("tft_item_name_")
                && !normalized.startsWith("tft_item_description_")
                && !normalized.startsWith("tft_augment_")
                && !normalized.startsWith("tftaugment_");
    }

    private record GuideCandidate(
            GuideType guideType,
            String targetKey,
            String name,
            String summary,
            String imageUrl,
            JsonNode dataJson,
            String patchVersion,
            int sortOrder
    ) {
    }

    private record ResolvedCdragonSet(
            JsonNode data,
            int setNumber,
            String mutator
    ) {
    }

    private record GuideImportSource(
            int setNumber,
            String mutator
    ) {
    }

    private record GuideImportCounts(
            int championCount,
            int traitCount,
            int itemCount,
            int augmentCount
    ) {
    }

    private record PatchVersionParts(int major, int minor, String suffix) {
    }

    private static class ImportCounter {
        private int createdCount;
        private int updatedCount;
        private int skippedCount;

        private void add(ImportResult result) {
            switch (result) {
                case CREATED -> createdCount++;
                case UPDATED -> updatedCount++;
                case SKIPPED -> skippedCount++;
            }
        }
    }

    private enum ImportResult {
        CREATED,
        UPDATED,
        SKIPPED
    }
}
