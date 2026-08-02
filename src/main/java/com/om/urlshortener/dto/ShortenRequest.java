package com.om.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for shortening a URL")
public record ShortenRequest(
		@Schema(description = "The long URL to be shortened", example = "https://example.com/some/very/long/path?query=1")
		@NotBlank(message = "longUrl is required") String longUrl
) {
}

