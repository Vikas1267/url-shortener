package com.om.urlshortener.integration;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class RedirectFlowIT {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
			.withDatabaseName("urlshortener")
			.withUsername("urlshortener")
			.withPassword("urlshortener");

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("app.base-url", () -> "http://localhost:8080");
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void fullShortenRedirectStatsFlowUsesRealPostgres() throws Exception {
		MvcResult shortenResult = mockMvc.perform(post("/api/shorten")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"longUrl\":\"https://example.com/products/widget?ref=portfolio\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.shortCode").isString())
				.andExpect(jsonPath("$.shortUrl").isString())
				.andReturn();

		String json = shortenResult.getResponse().getContentAsString();
		String shortCode = JsonPath.read(json, "$.shortCode");
		String shortUrl = JsonPath.read(json, "$.shortUrl");
		assertThat(shortUrl).isEqualTo("http://localhost:8080/" + shortCode);

		mockMvc.perform(get("/" + shortCode)
						.header("Referer", "https://twitter.com")
						.header("User-Agent", "IntegrationTest")
						.header("X-Forwarded-For", "203.0.113.42"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/products/widget?ref=portfolio"));

		await().atMost(ofSeconds(5)).untilAsserted(() ->
				mockMvc.perform(get("/api/stats/" + shortCode))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.shortCode").value(shortCode))
						.andExpect(jsonPath("$.totalClicks").value(1))
						.andExpect(jsonPath("$.clicksPerDay[0].count").value(1))
						.andExpect(jsonPath("$.referrers[0].referrer").value("https://twitter.com"))
						.andExpect(jsonPath("$.referrers[0].count").value(1))
		);
	}

	@Test
	void databaseUniqueConstraintRejectsDuplicateShortCode() {
		jdbcTemplate.update("INSERT INTO urls (short_code, long_url) VALUES (?, ?)",
				"dupe1", "https://example.com/first");

		assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO urls (short_code, long_url) VALUES (?, ?)",
				"dupe1", "https://example.com/second"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
