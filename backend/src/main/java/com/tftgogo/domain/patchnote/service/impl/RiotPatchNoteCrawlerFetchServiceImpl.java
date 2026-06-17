package com.tftgogo.domain.patchnote.service.impl;

import com.tftgogo.domain.patchnote.config.PatchNoteCrawlerProperties;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerFetchService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class RiotPatchNoteCrawlerFetchServiceImpl implements PatchNoteCrawlerFetchService {

    private static final Logger logger = LogManager.getLogger(RiotPatchNoteCrawlerFetchServiceImpl.class);
    private static final Set<String> OFFICIAL_HOSTS = Set.of(
            "www.leagueoflegends.com",
            "teamfighttactics.leagueoflegends.com"
    );
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[a-z]{2}-[a-z]{2}$");

    private final RestTemplate restTemplate;
    private final PatchNoteCrawlerProperties properties;

    public RiotPatchNoteCrawlerFetchServiceImpl(PatchNoteCrawlerProperties properties) {
        this(createRestTemplate(properties), properties);
    }

    RiotPatchNoteCrawlerFetchServiceImpl(RestTemplate restTemplate, PatchNoteCrawlerProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    private static RestTemplate createRestTemplate(PatchNoteCrawlerProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMillis());
        factory.setReadTimeout(properties.getReadTimeoutMillis());
        return new RestTemplate(factory);
    }

    @Override
    public PatchNoteCrawlFetchedPage fetch(String sourceUrl) {
        URI sourceUri = validateOfficialUrl(sourceUrl);
        String normalizedUrl = sourceUri.toString();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, properties.getUserAgent());
            headers.setAccept(List.of(MediaType.TEXT_HTML, MediaType.ALL));

            ResponseEntity<String> response = restTemplate.exchange(
                    normalizedUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                logger.warn("Patch note crawl returned empty body. sourceUrl={}", normalizedUrl);
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
            }

            return new PatchNoteCrawlFetchedPage(
                    normalizedUrl,
                    body,
                    LocalDateTime.now(),
                    response.getStatusCode().value()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Patch note crawl source not found. sourceUrl={}", normalizedUrl);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        } catch (RestClientException e) {
            logger.error("Failed to fetch official patch note page. sourceUrl={}, error={}",
                    normalizedUrl, e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    @Override
    public PatchNoteCrawlFetchedPage fetchTagPage(String locale) {
        String resolvedLocale = normalizeLocale(locale);
        String defaultLocale = normalizeLocale(properties.getDefaultLocale());
        String tagUrl = properties.getTagUrl().replace(
                "/" + defaultLocale + "/",
                "/" + resolvedLocale + "/"
        );
        return fetch(tagUrl);
    }

    private String normalizeLocale(String locale) {
        String resolvedLocale = hasText(locale) ? locale.trim() : properties.getDefaultLocale();
        String normalizedLocale = resolvedLocale.toLowerCase(Locale.ROOT);
        if (!LOCALE_PATTERN.matcher(normalizedLocale).matches()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return normalizedLocale;
    }

    private URI validateOfficialUrl(String sourceUrl) {
        if (!hasText(sourceUrl)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        try {
            URI uri = new URI(sourceUrl.trim()).normalize();
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(scheme) || host == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }

            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if (!OFFICIAL_HOSTS.contains(normalizedHost)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
