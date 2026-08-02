package com.om.urlshortener.exception;

public class UrlGoneException extends RuntimeException {

	public UrlGoneException(String shortCode) {
		super("Short code is expired or deactivated: " + shortCode);
	}
}
