package com.om.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resolved URL mapping response payload")
public record ResolveResponse(
		@Schema(description = "Base62 short code", example = "q0V") String shortCode,
		@Schema(description = "Target long URL destination", example = "https://example.com/some/very/long/path?query=1") String longUrl
) {
}
