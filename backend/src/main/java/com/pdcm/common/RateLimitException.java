package com.pdcm.common;

public class RateLimitException extends RuntimeException {

	private final String limit;

	public RateLimitException(String message, String limit) {
		super(message);
		this.limit = limit;
	}

	public String getLimit() {
		return limit;
	}
}
