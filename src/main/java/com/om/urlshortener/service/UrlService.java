package com.om.urlshortener.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.om.urlshortener.dto.ShortenResponse;
import com.om.urlshortener.entity.Url;
import com.om.urlshortener.exception.InvalidUrlException;
import com.om.urlshortener.exception.UrlGoneException;
import com.om.urlshortener.exception.UrlNotFoundException;
import com.om.urlshortener.repository.UrlRepository;
import com.om.urlshortener.util.Base62;

@Service
public class UrlService {

	private static final Logger log = LoggerFactory.getLogger(UrlService.class);
	private static final int MAX_URL_LENGTH = 2048;

	private final UrlRepository urlRepository;
	private final Clock clock;
	private final String baseUrl;
	private final long shortCodeOffset;

	public UrlService(
			UrlRepository urlRepository,
			Clock clock,
			@Value("${app.base-url}") String baseUrl,
			@Value("${app.short-code.offset}") long shortCodeOffset
	) {
		this.urlRepository = urlRepository;
		this.clock = clock;
		this.baseUrl = normalizeBaseUrl(baseUrl);
		this.shortCodeOffset = shortCodeOffset;
	}

	@Transactional
	public ShortenResponse shorten(String longUrl) {
		String normalizedLongUrl = validateLongUrl(longUrl);

		Url url = Url.pending(normalizedLongUrl);
		urlRepository.saveAndFlush(url);

		long codeSource = Math.addExact(url.getId(), shortCodeOffset);
		String shortCode = Base62.encode(codeSource);
		url.setShortCode(shortCode);

		log.info("url_shortened shortCode={} urlId={}", shortCode, url.getId());
		return new ShortenResponse(shortCode, shortUrl(shortCode));
	}

	@Transactional(readOnly = true)
	public Url resolveForRedirect(String shortCode) {
		Url url = findByShortCode(shortCode);
		if (!url.isActive() || isExpired(url)) {
			throw new UrlGoneException(shortCode);
		}
		log.info("redirect_resolved shortCode={} urlId={} longUrl={}", shortCode, url.getId(), url.getLongUrl());
		return url;
	}

	@Transactional(readOnly = true)
	public Url findByShortCode(String shortCode) {
		return urlRepository.findByShortCode(shortCode)
				.orElseThrow(() -> new UrlNotFoundException(shortCode));
	}

	private String validateLongUrl(String longUrl) {
		if (longUrl == null || longUrl.isBlank()) {
			throw new InvalidUrlException("longUrl must be a non-blank absolute URL");
		}

		String candidate = longUrl.trim();
		if (candidate.length() > MAX_URL_LENGTH) {
			throw new InvalidUrlException("longUrl must be 2048 characters or fewer");
		}
		if (containsControlCharacter(candidate)) {
			throw new InvalidUrlException("longUrl contains invalid control characters");
		}

		URI uri;
		try {
			uri = new URI(candidate);
		}
		catch (URISyntaxException ex) {
			throw new InvalidUrlException("longUrl must be a valid absolute URL");
		}

		String scheme = uri.getScheme();
		if (!uri.isAbsolute() || scheme == null || uri.getHost() == null) {
			throw new InvalidUrlException("longUrl must be an absolute URL");
		}
		String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
		if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) {
			throw new InvalidUrlException("longUrl scheme must be http or https");
		}

		return uri.toString();
	}

	private boolean containsControlCharacter(String value) {
		for (int i = 0; i < value.length(); i++) {
			if (Character.isISOControl(value.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	private boolean isExpired(Url url) {
		OffsetDateTime expiresAt = url.getExpiresAt();
		return expiresAt != null && !expiresAt.isAfter(OffsetDateTime.now(clock));
	}

	private String shortUrl(String shortCode) {
		return baseUrl + "/" + shortCode;
	}

	private String normalizeBaseUrl(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("app.base-url is required");
		}
		String normalized = value.trim();
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}
}
