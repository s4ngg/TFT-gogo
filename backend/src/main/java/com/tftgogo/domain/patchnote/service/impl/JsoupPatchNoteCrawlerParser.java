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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JsoupPatchNoteCrawlerParser implements PatchNoteCrawlerParser {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d{1,2})[.-](\\d{1,2}[a-zA-Z]?)");
    private static final Pattern SLUG_VERSION_PATTERN = Pattern.compile("patch-(\\d{1,2})-(\\d{1,2}[a-zA-Z]?)");

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
        Elements elements = container.select("h2, h3, h4, li");
        for (Element element : elements) {
            String tagName = element.tagName();
            if ("h2".equals(tagName)) {
                currentSection = normalizeText(element.text());
                currentGroup = "";
                continue;
            }
            if ("h3".equals(tagName) || "h4".equals(tagName)) {
                currentGroup = normalizeText(element.text());
                continue;
            }
            if (!"li".equals(tagName)) {
                continue;
            }
            if (rows.size() >= properties.getMaxDetailRows()) {
                parserWarnings.add("max detail rows reached");
                break;
            }

            String rowText = normalizeText(element.text());
            if (!hasText(rowText)) {
                continue;
            }

            int sourceOrder = rows.size();
            List<String> rowWarnings = new ArrayList<>();
            if (!hasText(currentSection) && !hasText(currentGroup)) {
                rowWarnings.add("missing heading");
            }
            if (!element.children().select("li").isEmpty()) {
                rowWarnings.add("nested list");
            }

            String headingPath = joinHeadingPath(currentSection, currentGroup);
            BeforeAfterPair beforeAfterPair = extractBeforeAfter(element, rowWarnings);
            String sourceKeyCandidate = buildSourceKeyCandidate(
                    sourceUrl,
                    contentId,
                    headingPath,
                    sourceOrder,
                    rowText
            );

            rows.add(new PatchChangeCrawlRow(
                    sourceKeyCandidate,
                    sha256(sourceKeyCandidate),
                    headingPath,
                    sourceOrder,
                    currentSection,
                    currentGroup,
                    rowText,
                    element.outerHtml(),
                    beforeAfterPair.beforeText(),
                    beforeAfterPair.afterText(),
                    rowWarnings
            ));
        }
        return rows;
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

    private String joinHeadingPath(String sectionTitle, String groupTitle) {
        if (hasText(sectionTitle) && hasText(groupTitle)) {
            return sectionTitle + " > " + groupTitle;
        }
        if (hasText(sectionTitle)) {
            return sectionTitle;
        }
        return defaultString(groupTitle);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private record BeforeAfterPair(String beforeText, String afterText) {
    }
}
