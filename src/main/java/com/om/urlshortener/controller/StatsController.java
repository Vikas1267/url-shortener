package com.om.urlshortener.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.om.urlshortener.dto.StatsResponse;
import com.om.urlshortener.exception.GlobalExceptionHandler;
import com.om.urlshortener.service.StatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Analytics", description = "Endpoints for retrieving URL click analytics")
@RestController
@RequestMapping("/api")
public class StatsController {

	private final StatsService statsService;

	public StatsController(StatsService statsService) {
		this.statsService = statsService;
	}

	@Operation(summary = "Get click statistics", description = "Retrieves total click count, daily click history, and referrer breakdown for a given short code.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Analytics retrieved successfully",
					content = @Content(schema = @Schema(implementation = StatsResponse.class))),
			@ApiResponse(responseCode = "404", description = "Short code not found",
					content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)))
	})
	@GetMapping("/stats/{shortCode:[0-9A-Za-z]{1,10}}")
	StatsResponse stats(@Parameter(description = "Base62 short code", example = "q0V") @PathVariable String shortCode) {
		return statsService.getStats(shortCode);
	}
}

