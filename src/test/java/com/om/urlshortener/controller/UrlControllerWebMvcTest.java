package com.om.urlshortener.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.om.urlshortener.dto.ShortenResponse;
import com.om.urlshortener.dto.StatsResponse;
import com.om.urlshortener.entity.Url;
import com.om.urlshortener.exception.GlobalExceptionHandler;
import com.om.urlshortener.exception.InvalidUrlException;
import com.om.urlshortener.exception.UrlGoneException;
import com.om.urlshortener.exception.UrlNotFoundException;
import com.om.urlshortener.service.ClickService;
import com.om.urlshortener.service.StatsService;
import com.om.urlshortener.service.UrlService;

@WebMvcTest(controllers = {ShortenController.class, RedirectController.class, StatsController.class})
@Import(GlobalExceptionHandler.class)
class UrlControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UrlService urlService;

	@MockitoBean
	private ClickService clickService;

	@MockitoBean
	private StatsService statsService;

	@Test
	void shortenReturns201ForValidRequest() throws Exception {
		when(urlService.shorten("https://example.com/long/path"))
				.thenReturn(new ShortenResponse("abc", "http://localhost:8080/abc"));

		mockMvc.perform(post("/api/shorten")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"longUrl\":\"https://example.com/long/path\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.shortCode").value("abc"))
				.andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc"));
	}

	@Test
	void shortenReturns400WhenLongUrlIsMissing() throws Exception {
		mockMvc.perform(post("/api/shorten")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").exists())
				.andExpect(jsonPath("$.timestamp").exists());

		verifyNoInteractions(urlService);
	}

	@Test
	void shortenReturns400ForMalformedJson() throws Exception {
		mockMvc.perform(post("/api/shorten")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"longUrl\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Malformed JSON request body"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void shortenReturns422ForSemanticallyInvalidUrl() throws Exception {
		when(urlService.shorten("not-a-url"))
				.thenThrow(new InvalidUrlException("longUrl must be an absolute URL"));

		mockMvc.perform(post("/api/shorten")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"longUrl\":\"not-a-url\"}"))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.status").value(422))
				.andExpect(jsonPath("$.error").value("longUrl must be an absolute URL"));
	}

	@Test
	void shortenReturns500ForUnexpectedFailure() throws Exception {
		when(urlService.shorten("https://example.com"))
				.thenThrow(new RuntimeException("database down"));

		mockMvc.perform(post("/api/shorten")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"longUrl\":\"https://example.com\"}"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.error").value("Unexpected server error"));
	}

	@Test
	void redirectReturns302WithLocationAndEmptyBody() throws Exception {
		Url url = url("abc", "https://example.com/final");
		when(urlService.resolveForRedirect("abc")).thenReturn(url);

		mockMvc.perform(get("/abc")
						.header("Referer", "https://twitter.com")
						.header("User-Agent", "JUnit")
						.header("X-Forwarded-For", "203.0.113.10, 10.0.0.1"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/final"))
				.andExpect(content().string(""));

		verify(clickService).logClick(url, "https://twitter.com", "JUnit", "203.0.113.10");
	}

	@Test
	void redirectReturns404WhenCodeDoesNotExist() throws Exception {
		when(urlService.resolveForRedirect("missing"))
				.thenThrow(new UrlNotFoundException("missing"));

		mockMvc.perform(get("/missing"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Short code not found: missing"));
	}

	@Test
	void redirectReturns410WhenCodeIsExpiredOrInactive() throws Exception {
		when(urlService.resolveForRedirect("expired"))
				.thenThrow(new UrlGoneException("expired"));

		mockMvc.perform(get("/expired"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.status").value(410))
				.andExpect(jsonPath("$.error").value("Short code is expired or deactivated: expired"));
	}

	@Test
	void statsReturns200ForExistingCode() throws Exception {
		when(statsService.getStats("abc")).thenReturn(new StatsResponse(
				"abc",
				2,
				List.of(new StatsResponse.ClicksPerDay(LocalDate.parse("2026-08-02"), 2)),
				List.of(new StatsResponse.ReferrerCount("direct", 1),
						new StatsResponse.ReferrerCount("https://twitter.com", 1))
		));

		mockMvc.perform(get("/api/stats/abc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shortCode").value("abc"))
				.andExpect(jsonPath("$.totalClicks").value(2))
				.andExpect(jsonPath("$.clicksPerDay[0].date").value("2026-08-02"))
				.andExpect(jsonPath("$.clicksPerDay[0].count").value(2))
				.andExpect(jsonPath("$.referrers[0].referrer").value("direct"))
				.andExpect(jsonPath("$.referrers[0].count").value(1));
	}

	@Test
	void statsReturns404WhenCodeDoesNotExist() throws Exception {
		when(statsService.getStats("missing"))
				.thenThrow(new UrlNotFoundException("missing"));

		mockMvc.perform(get("/api/stats/missing"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Short code not found: missing"));
	}

	private Url url(String shortCode, String longUrl) {
		Url url = Url.pending(longUrl);
		url.setId(1L);
		url.setShortCode(shortCode);
		return url;
	}
}
