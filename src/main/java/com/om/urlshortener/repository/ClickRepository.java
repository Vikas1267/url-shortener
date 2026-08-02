package com.om.urlshortener.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.om.urlshortener.entity.Click;

public interface ClickRepository extends JpaRepository<Click, Long> {

	long countByUrlId(Long urlId);

	@Query(value = """
			SELECT CAST(clicked_at AT TIME ZONE 'UTC' AS date) AS clickDate,
			       COUNT(*) AS clickCount
			FROM clicks
			WHERE url_id = :urlId
			GROUP BY CAST(clicked_at AT TIME ZONE 'UTC' AS date)
			ORDER BY clickDate
			""", nativeQuery = true)
	List<DailyClickCount> countClicksPerDay(@Param("urlId") Long urlId);

	@Query(value = """
			SELECT COALESCE(NULLIF(referrer, ''), 'direct') AS referrerValue,
			       COUNT(*) AS clickCount
			FROM clicks
			WHERE url_id = :urlId
			GROUP BY COALESCE(NULLIF(referrer, ''), 'direct')
			ORDER BY clickCount DESC, referrerValue ASC
			""", nativeQuery = true)
	List<ReferrerClickCount> countClicksByReferrer(@Param("urlId") Long urlId);

	interface DailyClickCount {
		LocalDate getClickDate();

		long getClickCount();
	}

	interface ReferrerClickCount {
		String getReferrerValue();

		long getClickCount();
	}
}
