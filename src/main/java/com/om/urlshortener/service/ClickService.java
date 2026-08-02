package com.om.urlshortener.service;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.om.urlshortener.entity.Click;
import com.om.urlshortener.entity.Url;
import com.om.urlshortener.repository.ClickRepository;

@Service
public class ClickService {

	private static final Logger log = LoggerFactory.getLogger(ClickService.class);

	private final ClickRepository clickRepository;
	private final Executor clickTaskExecutor;

	public ClickService(ClickRepository clickRepository, @Qualifier("clickTaskExecutor") Executor clickTaskExecutor) {
		this.clickRepository = clickRepository;
		this.clickTaskExecutor = clickTaskExecutor;
	}

	public void logClick(Url url, String referrer, String userAgent, String ipAddress) {
		if (url == null || url.getId() == null) {
			log.warn("click_log_skipped reason=missing_url");
			return;
		}

		Long urlId = url.getId();
		String shortCode = url.getShortCode();
		String normalizedReferrer = blankToNull(referrer);
		String normalizedUserAgent = blankToNull(userAgent);
		String normalizedIpAddress = blankToNull(ipAddress);

		try {
			clickTaskExecutor.execute(() -> persistClick(urlId, shortCode, normalizedReferrer, normalizedUserAgent,
					normalizedIpAddress));
		}
		catch (RuntimeException ex) {
			log.warn("click_log_schedule_failed shortCode={} urlId={}", shortCode, urlId, ex);
		}
	}

	private void persistClick(Long urlId, String shortCode, String referrer, String userAgent, String ipAddress) {
		try {
			clickRepository.save(new Click(urlId, referrer, ipAddress, userAgent));
			log.info("click_logged shortCode={} urlId={}", shortCode, urlId);
		}
		catch (Exception ex) {
			log.warn("click_log_failed shortCode={} urlId={}", shortCode, urlId, ex);
		}
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
