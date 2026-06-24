package com.tftgogo.domain.patchnote.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.patchnote.config.PatchNoteCrawlerProperties;
import com.tftgogo.domain.patchnote.dto.crawl.PatchChangeCrawlRow;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlDocument;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlListItem;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerParser;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JsoupPatchNoteCrawlerParser implements PatchNoteCrawlerParser {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d{1,2})[.-](\\d{1,2}[a-zA-Z]?)");
    private static final Pattern SLUG_VERSION_PATTERN = Pattern.compile("patch-(\\d{1,2})-(\\d{1,2}[a-zA-Z]?)");
    private static final Set<String> GENERIC_HEADING_TITLES = Set.of(
            "augment",
            "augments",
            "balance changes",
            "bug fix",
            "bug fixes",
            "champion",
            "champions",
            "item",
            "items",
            "large changes",
            "small changes",
            "system",
            "systems",
            "trait",
            "traits",
            "unit",
            "units",
            "대규모 변경",
            "밸런스 변경",
            "밸런스 변경 사항",
            "버그 수정",
            "버그 수정 사항",
            "변경 사항",
            "변경사항",
            "소규모 변경",
            "시너지",
            "시스템",
            "아이템",
            "유닛",
            "증강",
            "증강체",
            "챔피언",
            "특성"
    );

    private final ObjectMapper objectMapper;
    private final PatchNoteCrawlerProperties properties;

    @Override
    public List<PatchNoteCrawlListItem> parseListPage(PatchNoteCrawlFetchedPage fetchedPage) {
        JsonNode page = readPage(fetchedPage.rawHtml());
        JsonNode articleCardGrid = findBlade(page, "articleCardGrid")
                .orElseThrow(() -> new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA));

        List<PatchNoteCrawlListItem> items = new ArrayList<>();
        for (JsonNode item : articleCardGrid.path("items")) {
            String title = readText(item, "title");
            String detailUrl = resolveUrl(fetchedPage.sourceUrl(), readText(item.path("action").path("payload"), "url"));
            if (!hasText(title) || !hasText(detailUrl)) {
                continue;
            }

            items.add(new PatchNoteCrawlListItem(
                    title,
                    parseDateTime(readText(item, "publishedAt")),
                    stripHtml(readText(item.path("description"), "body")),
                    readFirstText(item, "imageMedia.url", "image.url", "banner.url"),
                    readFirstText(item, "analytics.contentId", "contentId"),
                    detailUrl
            ));
        }
        return items;
    }

    @Override
    public PatchNoteCrawlDocument parseDetailPage(
            PatchNoteCrawlFetchedPage fetchedPage,
            String explicitVersion,
            String locale) {
        JsonNode page = readPage(fetchedPage.rawHtml());
        List<String> parserWarnings = new ArrayList<>();
        JsonNode masthead = findBlade(page, "articleMasthead").orElse(null);
        JsonNode richTextBlade = findBlade(page, "patchNotesRichText")
                .orElseThrow(() -> new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA));
        String richTextBody = readText(richTextBlade.path("richText"), "body");
        if (!hasText(richTextBody)) {
            throw new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA);
        }

        String title = masthead != null ? readText(masthead, "title") : readText(page, "title");
        if (!hasText(title)) {
            parserWarnings.add("missing title");
        }

        String contentId = readFirstText(page, "analytics.contentId", "contentId");
        if (!hasText(contentId) && masthead != null) {
            contentId = readFirstText(masthead, "analytics.contentId", "contentId");
        }

        String resolvedLocale = hasText(locale) ? locale.trim() : properties.getDefaultLocale();
        List<PatchChangeCrawlRow> rows = parseRows(
                fetchedPage.sourceUrl(),
                contentId,
                richTextBody,
                parserWarnings
        );

        return new PatchNoteCrawlDocument(
                fetchedPage.sourceUrl(),
                resolvedLocale,
                contentId,
                title,
                resolveVersion(explicitVersion, title, fetchedPage.sourceUrl()),
                masthead != null ? stripHtml(readText(masthead.path("description"), "body")) : "",
                masthead != null ? parseDateTime(readFirstText(masthead, "publishDate", "publishedAt")) : null,
                masthead != null ? readFirstText(masthead, "image.url", "imageMedia.url", "banner.url") : "",
                masthead != null ? readAuthors(masthead) : List.of(),
                rows.stream()
                        .map(PatchChangeCrawlRow::headingPath)
                        .filter(this::hasText)
                        .distinct()
                        .toList(),
                rows,
                parserWarnings
        );
    }

    private List<PatchChangeCrawlRow> parseRows(
            String sourceUrl,
            String contentId,
            String richTextBody,
            List<String> parserWarnings) {
        Document bodyDocument = Jsoup.parseBodyFragment(richTextBody);
        Element container = bodyDocument.selectFirst("#patch-notes-container");
        if (container == null) {
            container = bodyDocument.body();
            parserWarnings.add("missing patch-notes-container");
        }

        List<PatchChangeCrawlRow> rows = new ArrayList<>();
        String currentSection = "";
        String currentGroup = "";
        String currentSubGroup = "";
        Elements elements = container.select("h2, h3, h4, li");
        for (Element element : elements) {
            String tagName = element.tagName();
            if ("h2".equals(tagName)) {
                currentSection = normalizeText(element.text());
                currentGroup = "";
                currentSubGroup = "";
                continue;
            }
            if ("h3".equals(tagName)) {
                currentGroup = normalizeText(element.text());
                currentSubGroup = "";
                continue;
            }
            if ("h4".equals(tagName)) {
                currentSubGroup = normalizeText(element.text());
                continue;
            }
            if (!"li".equals(tagName)) {
                continue;
            }
            if (rows.size() >= properties.getMaxDetailRows()) {
                parserWarnings.add("max detail rows reached");
                break;
            }
            if (hasNestedListItems(element)) {
                continue;
            }

            RowContext rowContext = resolveRowContext(currentSection, currentGroup, currentSubGroup, element, container);
            String rowText = rowContext.rowText();
            if (!hasText(rowText)) {
                continue;
            }

            int sourceOrder = rows.size();
            List<String> rowWarnings = new ArrayList<>();
            if (!hasText(currentSection) && !hasText(rowContext.groupTitle())) {
                rowWarnings.add("missing heading");
            }

            BeforeAfterPair beforeAfterPair = extractBeforeAfter(element, rowWarnings);
            String sourceKeyCandidate = buildSourceKeyCandidate(
                    sourceUrl,
                    contentId,
                    rowContext.headingPath(),
                    sourceOrder,
                    rowText
            );

            rows.add(new PatchChangeCrawlRow(
                    sourceKeyCandidate,
                    sha256(sourceKeyCandidate),
                    rowContext.headingPath(),
                    sourceOrder,
                    currentSection,
                    rowContext.groupTitle(),
                    rowText,
                    element.outerHtml(),
                    beforeAfterPair.beforeText(),
                    beforeAfterPair.afterText(),
                    rowWarnings
            ));
        }
        return rows;
    }

    private RowContext resolveRowContext(
            String currentSection,
            String currentGroup,
            String currentSubGroup,
            Element rowElement,
            Element container) {
        List<String> ancestorContexts = listItemAncestorTexts(rowElement, container);
        List<String> detailPrefixes = new ArrayList<>();
        String groupTitle = "";

        if (hasText(currentGroup) && hasText(currentSubGroup)) {
            if (isGenericHeadingTitle(currentGroup)) {
                groupTitle = currentSubGroup;
            } else {
                groupTitle = currentGroup;
                detailPrefixes.add(currentSubGroup);
            }
        } else if (hasText(currentSubGroup)) {
            groupTitle = currentSubGroup;
        } else if (hasText(currentGroup)) {
            groupTitle = currentGroup;
        }

        if (!ancestorContexts.isEmpty()) {
            if (!hasText(groupTitle) || isGenericHeadingTitle(groupTitle)) {
                groupTitle = ancestorContexts.get(0);
                detailPrefixes.addAll(ancestorContexts.subList(1, ancestorContexts.size()));
            } else {
                detailPrefixes.addAll(ancestorContexts);
            }
        }

        String ownText = ownText(rowElement);
        String rowText = prependDetailPrefixes(ownText, detailPrefixes);
        String headingPath = joinHeadingPath(currentSection, currentGroup, currentSubGroup, ancestorContexts);
        return new RowContext(headingPath, groupTitle, rowText);
    }

    private JsonNode readPage(String rawHtml) {
        if (!hasText(rawHtml)) {
            throw new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA);
        }

        Document document = Jsoup.parse(rawHtml);
        Element nextData = document.selectFirst("script#__NEXT_DATA__");
        if (nextData == null || !hasText(nextData.data())) {
            throw new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA);
        }

        try {
            return objectMapper.readTree(nextData.data())
                    .path("props")
                    .path("pageProps")
                    .path("page");
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA);
        }
    }

    private Optional<JsonNode> findBlade(JsonNode page, String type) {
        for (JsonNode blade : page.path("blades")) {
            if (type.equals(blade.path("type").asText())) {
                return Optional.of(blade);
            }
        }
        return Optional.empty();
    }

    private List<String> readAuthors(JsonNode masthead) {
        List<String> authors = new ArrayList<>();
        for (JsonNode author : masthead.path("authors")) {
            String name = readText(author, "name");
            if (hasText(name)) {
                authors.add(name);
            }
        }
        return authors;
    }

    private String resolveVersion(String explicitVersion, String title, String sourceUrl) {
        if (hasText(explicitVersion)) {
            return explicitVersion.trim();
        }

        Matcher titleMatcher = VERSION_PATTERN.matcher(defaultString(title));
        if (titleMatcher.find()) {
            return titleMatcher.group(1) + "." + titleMatcher.group(2);
        }

        Matcher slugMatcher = SLUG_VERSION_PATTERN.matcher(defaultString(sourceUrl));
        if (slugMatcher.find()) {
            return slugMatcher.group(1) + "." + slugMatcher.group(2);
        }

        return "";
    }

    private BeforeAfterPair extractBeforeAfter(Element rowElement, List<String> rowWarnings) {
        Elements indicators = rowElement.select("span.change-indicator, span.change-indicator-separated");
        if (indicators.isEmpty()) {
            return new BeforeAfterPair(null, null);
        }
        if (indicators.size() > 1) {
            rowWarnings.add("multiple change indicators");
            return new BeforeAfterPair(null, null);
        }

        String rowText = normalizeText(rowElement.text());
        String indicatorText = normalizeText(indicators.first().text());
        if (!hasText(indicatorText) || !rowText.contains(indicatorText)) {
            rowWarnings.add("uncertain before/after split");
            return new BeforeAfterPair(null, null);
        }

        String[] parts = rowText.split(Pattern.quote(indicatorText), 2);
        if (parts.length != 2 || !hasText(parts[0]) || !hasText(parts[1])) {
            rowWarnings.add("uncertain before/after split");
            return new BeforeAfterPair(null, null);
        }

        return new BeforeAfterPair(normalizeText(parts[0]), normalizeText(parts[1]));
    }

    private String buildSourceKeyCandidate(
            String sourceUrl,
            String contentId,
            String headingPath,
            int sourceOrder,
            String rowText) {
        return String.join("|",
                defaultString(sourceUrl),
                defaultString(contentId),
                defaultString(headingPath),
                String.valueOf(sourceOrder),
                defaultString(rowText)
        ).toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (!hasText(value)) {
            return null;
        }

        String normalized = value.trim();
        try {
            return OffsetDateTime.parse(normalized).toLocalDateTime();
        } catch (RuntimeException ignored) {
            // try LocalDateTime below
        }
        try {
            return LocalDateTime.parse(normalized);
        } catch (RuntimeException ignored) {
            // try LocalDate below
        }
        try {
            return LocalDate.parse(normalized).atStartOfDay();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String resolveUrl(String baseUrl, String candidate) {
        if (!hasText(candidate)) {
            return "";
        }
        try {
            return URI.create(baseUrl).resolve(candidate.trim()).toString();
        } catch (RuntimeException e) {
            return candidate.trim();
        }
    }

    private String readFirstText(JsonNode node, String... paths) {
        for (String path : paths) {
            String value = readNestedText(node, path);
            if (hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String readNestedText(JsonNode node, String path) {
        JsonNode current = node;
        for (String segment : path.split("\\.")) {
            current = current.path(segment);
        }
        return current.isMissingNode() || current.isNull() ? "" : current.asText("");
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? "" : field.asText("");
    }

    private String stripHtml(String value) {
        if (!hasText(value)) {
            return "";
        }
        return normalizeText(Jsoup.parse(value).text());
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean hasNestedListItems(Element element) {
        return !element.select("> ul li, > ol li").isEmpty();
    }

    private List<String> listItemAncestorTexts(Element element, Element container) {
        List<String> ancestorTexts = new ArrayList<>();
        Element parent = element.parent();
        while (parent != null && parent != container) {
            if ("li".equals(parent.tagName())) {
                String parentText = ownText(parent);
                if (hasText(parentText)) {
                    ancestorTexts.add(0, parentText);
                }
            }
            parent = parent.parent();
        }
        return ancestorTexts;
    }

    private String ownText(Element element) {
        Element clone = element.clone();
        clone.select("ul, ol").remove();
        return normalizeText(clone.text());
    }

    private String prependDetailPrefixes(String text, List<String> detailPrefixes) {
        String normalizedText = normalizeText(text);
        List<String> prefixes = detailPrefixes.stream()
                .map(this::normalizeText)
                .filter(this::hasText)
                .distinct()
                .toList();
        if (prefixes.isEmpty()) {
            return normalizedText;
        }

        String prefix = String.join(" - ", prefixes);
        if (!hasText(normalizedText)) {
            return prefix;
        }

        String lowerText = normalizedText.toLowerCase(Locale.ROOT);
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        if (lowerText.equals(lowerPrefix)
                || lowerText.startsWith(lowerPrefix + ":")
                || lowerText.startsWith(lowerPrefix + " ")) {
            return normalizedText;
        }
        return prefix + ": " + normalizedText;
    }

    private String joinHeadingPath(
            String sectionTitle,
            String groupTitle,
            String subGroupTitle,
            List<String> ancestorTexts) {
        List<String> parts = new ArrayList<>();
        addHeadingPart(parts, sectionTitle);
        addHeadingPart(parts, groupTitle);
        addHeadingPart(parts, subGroupTitle);
        ancestorTexts.forEach(ancestorText -> addHeadingPart(parts, ancestorText));
        return String.join(" > ", parts);
    }

    private void addHeadingPart(List<String> parts, String value) {
        String normalizedValue = normalizeText(value);
        if (!hasText(normalizedValue)) {
            return;
        }
        if (!parts.isEmpty() && parts.get(parts.size() - 1).equals(normalizedValue)) {
            return;
        }
        parts.add(normalizedValue);
    }

    private boolean isGenericHeadingTitle(String value) {
        return GENERIC_HEADING_TITLES.contains(normalizeText(value).toLowerCase(Locale.ROOT));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private record BeforeAfterPair(String beforeText, String afterText) {
    }

    private record RowContext(String headingPath, String groupTitle, String rowText) {
    }
}
