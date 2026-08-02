package com.om.urlshortener.config;

import java.time.Clock;
import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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
			@Value("${spring.datasource.username:postgres}") String username,
			@Value("${spring.datasource.password:postgres}") String password
	) {
		String url = rawUrl != null ? rawUrl.trim() : "";
		if (url.startsWith("postgres://")) {
			url = "jdbc:postgresql://" + url.substring("postgres://".length());
		}
		else if (url.startsWith("postgresql://")) {
			url = "jdbc:postgresql://" + url.substring("postgresql://".length());
		}

		HikariConfig config = new HikariConfig();
		config.setDriverClassName("org.postgresql.Driver");
		config.setJdbcUrl(url);
		config.setUsername(username);
		config.setPassword(password);
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


