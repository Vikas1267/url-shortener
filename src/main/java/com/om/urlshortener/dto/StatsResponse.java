package com.om.urlshortener.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "URL Click Analytics Response")
public record StatsResponse(
		@Schema(description = "The short code", example = "q0V") String shortCode,
		@Schema(description = "Total number of clicks recorded", example = "42") long totalClicks,
		@Schema(description = "Daily click counts") List<ClicksPerDay> clicksPerDay,
		@Schema(description = "Click counts grouped by referrer") List<ReferrerCount> referrers
) {
	@Schema(description = "Daily click count breakdown")
	public record ClicksPerDay(
			@Schema(description = "Date of clicks in UTC", example = "2026-08-02") LocalDate date,
			@Schema(description = "Number of clicks on this date", example = "15") long count
	) {
	}

	@Schema(description = "Referrer click count breakdown")
	public record ReferrerCount(
			@Schema(description = "Referrer header domain/URL or 'direct'", example = "https://twitter.com") String referrer,
			@Schema(description = "Number of clicks from this referrer", example = "27") long count
	) {
	}
}

