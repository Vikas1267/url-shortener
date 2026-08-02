package com.om.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.om.urlshortener.dto.StatsResponse;
import com.om.urlshortener.entity.Url;
import com.om.urlshortener.exception.UrlNotFoundException;
import com.om.urlshortener.repository.ClickRepository;
import com.om.urlshortener.repository.UrlRepository;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

	@Mock
	private UrlRepository urlRepository;

	@Mock
	private ClickRepository clickRepository;

	@Test
	void aggregatesStatsRowsIntoApiShape() {
		Url url = Url.pending("https://example.com");
		url.setId(7L);
		url.setShortCode("abc");
		when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(url));
		when(clickRepository.countByUrlId(7L)).thenReturn(2L);
		when(clickRepository.countClicksPerDay(7L)).thenReturn(List.of(daily(LocalDate.parse("2026-08-02"), 2)));
		when(clickRepository.countClicksByReferrer(7L)).thenReturn(List.of(referrer("direct", 1), referrer("https://twitter.com", 1)));

		StatsResponse response = new StatsService(urlRepository, clickRepository).getStats("abc");

		assertThat(response.shortCode()).isEqualTo("abc");
		assertThat(response.totalClicks()).isEqualTo(2);
		assertThat(response.clicksPerDay()).containsExactly(new StatsResponse.ClicksPerDay(LocalDate.parse("2026-08-02"), 2));
		assertThat(response.referrers()).containsExactly(
				new StatsResponse.ReferrerCount("direct", 1),
				new StatsResponse.ReferrerCount("https://twitter.com", 1)
		);
	}

	@Test
	void throwsNotFoundForUnknownCode() {
		when(urlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> new StatsService(urlRepository, clickRepository).getStats("missing"))
				.isInstanceOf(UrlNotFoundException.class);
	}

	private ClickRepository.DailyClickCount daily(LocalDate date, long count) {
		return new ClickRepository.DailyClickCount() {
			@Override
			public LocalDate getClickDate() {
				return date;
			}

			@Override
			public long getClickCount() {
				return count;
			}
		};
	}

	private ClickRepository.ReferrerClickCount referrer(String referrer, long count) {
		return new ClickRepository.ReferrerClickCount() {
			@Override
			public String getReferrerValue() {
				return referrer;
			}

			@Override
			public long getClickCount() {
				return count;
			}
		};
	}
}
