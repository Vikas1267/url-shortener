package com.om.urlshortener.exception;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(this::formatFieldError)
				.collect(Collectors.joining("; "));
		if (message.isBlank()) {
			message = "Request body is invalid";
		}
		return error(HttpStatus.BAD_REQUEST, message, ex);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ErrorResponse> handleHandlerValidation(HandlerMethodValidationException ex) {
		return error(HttpStatus.BAD_REQUEST, "Request parameters are invalid", ex);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
		return error(HttpStatus.BAD_REQUEST, "Malformed JSON request body", ex);
	}

	@ExceptionHandler(InvalidUrlException.class)
	ResponseEntity<ErrorResponse> handleInvalidUrl(InvalidUrlException ex) {
		return error(HttpStatusCode.valueOf(422), ex.getMessage(), ex);
	}

	@ExceptionHandler(UrlNotFoundException.class)
	ResponseEntity<ErrorResponse> handleNotFound(UrlNotFoundException ex) {
		return error(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
	}

	@ExceptionHandler(UrlGoneException.class)
	ResponseEntity<ErrorResponse> handleGone(UrlGoneException ex) {
		return error(HttpStatus.GONE, ex.getMessage(), ex);
	}

	@ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
	ResponseEntity<ErrorResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
		return error(HttpStatus.NOT_FOUND, "Resource not found", ex);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", ex);
	}

	private String formatFieldError(FieldError error) {
		return error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getField() + " is invalid";
	}

	private ResponseEntity<ErrorResponse> error(HttpStatusCode status, String message, Exception ex) {
		if (status.is5xxServerError()) {
			log.error("request_failed status={} error={}", status.value(), message, ex);
		}
		else {
			log.warn("request_failed status={} error={}", status.value(), message);
		}
		return ResponseEntity.status(status)
				.body(new ErrorResponse(message, status.value(), OffsetDateTime.now(ZoneOffset.UTC)));
	}

	@io.swagger.v3.oas.annotations.media.Schema(description = "Standard API Error Response")
	public record ErrorResponse(
			@io.swagger.v3.oas.annotations.media.Schema(description = "Error detail message", example = "longUrl must be a valid absolute URL") String error,
			@io.swagger.v3.oas.annotations.media.Schema(description = "HTTP Status Code", example = "422") int status,
			@io.swagger.v3.oas.annotations.media.Schema(description = "Timestamp of error", example = "2026-08-02T11:40:00Z") OffsetDateTime timestamp
	) {
	}
}
