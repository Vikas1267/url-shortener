package com.om.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.om.urlshortener.dto.ShortenResponse;
import com.om.urlshortener.entity.Url;
import com.om.urlshortener.exception.InvalidUrlException;
import com.om.urlshortener.exception.UrlGoneException;
import com.om.urlshortener.exception.UrlNotFoundException;
import com.om.urlshortener.repository.UrlRepository;
import com.om.urlshortener.util.Base62;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-02T00:00:00Z"), ZoneOffset.UTC);

	@Mock
	private UrlRepository urlRepository;

	private UrlService urlService;

	@BeforeEach
	void setUp() {
		urlService = new UrlService(urlRepository, CLOCK, "http://localhost:8080/", 100000);
	}

	@Test
	void shortensValidatedUrlByEncodingDatabaseIdAfterInsert() {
		when(urlRepository.saveAndFlush(any(Url.class))).thenAnswer(invocation -> {
			Url url = invocation.getArgument(0);
			url.setId(1L);
			return url;
		});

		ShortenResponse response = urlService.shorten("https://example.com/some/path?query=1");

		String expectedCode = Base62.encode(100001);
		assertThat(response.shortCode()).isEqualTo(expectedCode);
		assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/" + expectedCode);

		ArgumentCaptor<Url> captor = ArgumentCaptor.forClass(Url.class);
		verify(urlRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getLongUrl()).isEqualTo("https://example.com/some/path?query=1");
		assertThat(captor.getValue().getShortCode()).isEqualTo(expectedCode);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"example.com/no-scheme",
			"https://exa mple.com",
			"javascript:alert(1)",
			"file:///tmp/example"
	})
	void rejectsSemanticallyInvalidUrlsWithoutTouchingRepository(String value) {
		assertThatThrownBy(() -> urlService.shorten(value))
				.isInstanceOf(InvalidUrlException.class);

		verifyNoInteractions(urlRepository);
	}

	@Test
	void rejectsBlankUrlWithoutTouchingRepository() {
		assertThatThrownBy(() -> urlService.shorten(" "))
				.isInstanceOf(InvalidUrlException.class);

		verifyNoInteractions(urlRepository);
	}

	@Test
	void rejectsTooLongUrlWithoutTouchingRepository() {
		String tooLong = "https://example.com/" + "a".repeat(2049);

		assertThatThrownBy(() -> urlService.shorten(tooLong))
				.isInstanceOf(InvalidUrlException.class)
				.hasMessageContaining("2048");

		verifyNoInteractions(urlRepository);
	}

	@Test
	void resolvesActiveUrlForRedirect() {
		Url url = url("abc", true, null);
		when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(url));

		assertThat(urlService.resolveForRedirect("abc")).isSameAs(url);
	}

	@Test
	void throwsNotFoundWhenCodeDoesNotExist() {
		when(urlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> urlService.resolveForRedirect("missing"))
				.isInstanceOf(UrlNotFoundException.class);
	}

	@Test
	void throwsGoneWhenUrlIsInactive() {
		when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(url("abc", false, null)));

		assertThatThrownBy(() -> urlService.resolveForRedirect("abc"))
				.isInstanceOf(UrlGoneException.class);
	}

	@Test
	void throwsGoneWhenUrlIsExpired() {
		OffsetDateTime expiresAt = OffsetDateTime.parse("2026-08-01T23:59:59Z");
		when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(url("abc", true, expiresAt)));

		assertThatThrownBy(() -> urlService.resolveForRedirect("abc"))
				.isInstanceOf(UrlGoneException.class);
	}

	private Url url(String shortCode, boolean active, OffsetDateTime expiresAt) {
		Url url = Url.pending("https://example.com");
		url.setId(1L);
		url.setShortCode(shortCode);
		url.setActive(active);
		url.setExpiresAt(expiresAt);
		return url;
	}
}
