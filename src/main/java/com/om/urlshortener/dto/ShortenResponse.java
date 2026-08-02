package com.om.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload after shortening a URL")
public record ShortenResponse(
		@Schema(description = "The generated Base62 short code", example = "q0V") String shortCode,
		@Schema(description = "The complete short URL", example = "http://localhost:8080/q0V") String shortUrl
) {
}

