package com.om.urlshortener.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.om.urlshortener.dto.StatsResponse;
import com.om.urlshortener.entity.Url;
import com.om.urlshortener.repository.ClickRepository;
import com.om.urlshortener.repository.UrlRepository;
import com.om.urlshortener.exception.UrlNotFoundException;

@Service
public class StatsService {

	private static final Logger log = LoggerFactory.getLogger(StatsService.class);

	private final UrlRepository urlRepository;
	private final ClickRepository clickRepository;

	public StatsService(UrlRepository urlRepository, ClickRepository clickRepository) {
		this.urlRepository = urlRepository;
		this.clickRepository = clickRepository;
	}

	@Transactional(readOnly = true)
	public StatsResponse getStats(String shortCode) {
		Url url = urlRepository.findByShortCode(shortCode)
				.orElseThrow(() -> new UrlNotFoundException(shortCode));

		long totalClicks = clickRepository.countByUrlId(url.getId());
		List<StatsResponse.ClicksPerDay> clicksPerDay = clickRepository.countClicksPerDay(url.getId()).stream()
				.map(row -> new StatsResponse.ClicksPerDay(row.getClickDate(), row.getClickCount()))
				.toList();
		List<StatsResponse.ReferrerCount> referrers = clickRepository.countClicksByReferrer(url.getId()).stream()
				.map(row -> new StatsResponse.ReferrerCount(row.getReferrerValue(), row.getClickCount()))
				.toList();

		log.info("stats_loaded shortCode={} urlId={} totalClicks={}", shortCode, url.getId(), totalClicks);
		return new StatsResponse(shortCode, totalClicks, clicksPerDay, referrers);
	}
}
