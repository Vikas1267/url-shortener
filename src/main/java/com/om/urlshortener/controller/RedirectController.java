package com.om.urlshortener.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.om.urlshortener.entity.Url;
import com.om.urlshortener.exception.GlobalExceptionHandler;
import com.om.urlshortener.service.ClickService;
import com.om.urlshortener.service.UrlService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Redirect", description = "Endpoint for HTTP 302 URL redirection")
@RestController
public class RedirectController {

	private final UrlService urlService;
	private final ClickService clickService;

	public RedirectController(UrlService urlService, ClickService clickService) {
		this.urlService = urlService;
		this.clickService = clickService;
	}

	@Operation(
			summary = "Redirect to target URL",
			description = "Resolves the Base62 short code, logs click analytics asynchronously, and returns HTTP 302 Found with the target long URL in the Location header. Pass `redirect=false` to return a JSON payload directly without performing a 302 browser redirect (ideal for Swagger UI testing)."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "302",
					description = "Redirect to target long URL (when redirect=true)",
					headers = @Header(name = "Location", description = "Target long URL destination", schema = @Schema(type = "string", example = "https://example.com/some/very/long/path?query=1"))
			),
			@ApiResponse(
					responseCode = "200",
					description = "Resolved URL JSON mapping (when redirect=false)",
					content = @Content(schema = @Schema(implementation = com.om.urlshortener.dto.ResolveResponse.class))
			),
			@ApiResponse(
					responseCode = "404",
					description = "Short code not found",
					content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))
			),
			@ApiResponse(
					responseCode = "410",
					description = "Short code expired or deactivated",
					content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))
			)
	})
	@GetMapping("/{shortCode:[0-9A-Za-z]{1,10}}")
	ResponseEntity<?> redirect(
			@Parameter(description = "Base62 short code", example = "q0V") @PathVariable String shortCode,
			@Parameter(description = "Set to false to return JSON response instead of HTTP 302 redirect (prevents CORS error in Swagger UI)", example = "true")
			@org.springframework.web.bind.annotation.RequestParam(name = "redirect", defaultValue = "true") boolean doRedirect,
			HttpServletRequest request
	) {
		Url url = urlService.resolveForRedirect(shortCode);
		clickService.logClick(url, request.getHeader("Referer"), request.getHeader("User-Agent"), clientIp(request));

		if (!doRedirect) {
			return ResponseEntity.ok(new com.om.urlshortener.dto.ResolveResponse(url.getShortCode(), url.getLongUrl()));
		}

		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(url.getLongUrl()))
				.build();
	}

	private String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",", 2)[0].trim();
		}
		String realIp = request.getHeader("X-Real-IP");
		if (realIp != null && !realIp.isBlank()) {
			return realIp.trim();
		}
		return request.getRemoteAddr();
	}
}

