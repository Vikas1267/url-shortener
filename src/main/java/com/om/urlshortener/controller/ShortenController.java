package com.om.urlshortener.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.om.urlshortener.dto.ShortenRequest;
import com.om.urlshortener.dto.ShortenResponse;
import com.om.urlshortener.exception.GlobalExceptionHandler;
import com.om.urlshortener.service.UrlService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.om.urlshortener.dto.ResolveResponse;
import com.om.urlshortener.entity.Url;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import io.swagger.v3.oas.annotations.Parameter;

@Tag(name = "URL Shortener", description = "Endpoints for shortening and resolving URLs")
@RestController
@RequestMapping("/api")
public class ShortenController {

	private final UrlService urlService;

	public ShortenController(UrlService urlService) {
		this.urlService = urlService;
	}

	@Operation(summary = "Shorten a URL", description = "Accepts an absolute HTTP/HTTPS URL and generates a Base62 short code.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "URL shortened successfully",
					content = @Content(schema = @Schema(implementation = ShortenResponse.class))),
			@ApiResponse(responseCode = "400", description = "Malformed or missing request body",
					content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))),
			@ApiResponse(responseCode = "422", description = "Semantic URL validation failure",
					content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)))
	})
	@PostMapping("/shorten")
	ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(urlService.shorten(request.longUrl()));
	}

	@Operation(summary = "Expand a short code", description = "Resolves a short code back to its original long URL without performing an HTTP redirect. Ideal for API consumers and Swagger UI testing.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Short code expanded successfully",
					content = @Content(schema = @Schema(implementation = ResolveResponse.class))),
			@ApiResponse(responseCode = "404", description = "Short code not found",
					content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))),
			@ApiResponse(responseCode = "410", description = "Short code expired or deactivated",
					content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)))
	})
	@GetMapping("/expand/{shortCode:[0-9A-Za-z]{1,10}}")
	ResponseEntity<ResolveResponse> expand(
			@Parameter(description = "Base62 short code", example = "q0V") @PathVariable String shortCode
	) {
		Url url = urlService.resolveForRedirect(shortCode);
		return ResponseEntity.ok(new ResolveResponse(url.getShortCode(), url.getLongUrl()));
	}
}


