package com.om.urlshortener.config;

import java.net.URI;
import java.time.Clock;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class AppConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	Executor clickTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(1000);
		executor.setThreadNamePrefix("click-log-");
		executor.initialize();
		return executor;
	}

	@Bean
	@Primary
	DataSource dataSource(
			@Value("${spring.datasource.url}") String rawUrl,
			@Value("${spring.datasource.username:postgres}") String fallbackUsername,
			@Value("${spring.datasource.password:postgres}") String fallbackPassword
	) {
		String url = rawUrl != null ? rawUrl.trim() : "";
		String finalUsername = fallbackUsername;
		String finalPassword = fallbackPassword;
		String finalJdbcUrl = url;

		if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
			try {
				String cleanUrl = url.startsWith("postgres://")
						? "http://" + url.substring("postgres://".length())
						: "http://" + url.substring("postgresql://".length());
				URI uri = new URI(cleanUrl);
				String host = uri.getHost();
				int port = uri.getPort() > 0 ? uri.getPort() : 5432;
				String path = uri.getPath();

				if (uri.getUserInfo() != null && uri.getUserInfo().contains(":")) {
					String[] parts = uri.getUserInfo().split(":", 2);
					finalUsername = parts[0];
					finalPassword = parts[1];
				}

				finalJdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
			}
			catch (Exception ex) {
				finalJdbcUrl = url.replaceFirst("^postgres(ql)?://", "jdbc:postgresql://");
			}
		}

		HikariConfig config = new HikariConfig();
		config.setDriverClassName("org.postgresql.Driver");
		config.setJdbcUrl(finalJdbcUrl);
		config.setUsername(finalUsername);
		config.setPassword(finalPassword);
		config.setConnectionTimeout(3000);
		config.setConnectionInitSql("SET statement_timeout TO '5s'");
		return new HikariDataSource(config);
	}

	@Bean
	WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
						.allowedOrigins("*")
						.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
						.allowedHeaders("*")
						.exposedHeaders("Location");
			}
		};
	}

	@Bean
	OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Snipr URL Shortener API")
						.version("1.0")
						.description("High-performance Spring Boot URL shortener REST API with Base62 encoding and click analytics."));
	}
}


