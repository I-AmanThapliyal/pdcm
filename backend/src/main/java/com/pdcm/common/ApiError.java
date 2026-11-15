package com.pdcm.common;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
		String code,
		String message,
		List<String> details,
		Instant timestamp,
		String limit
) {
	public static ApiError of(String code, String message) {
		return new ApiError(code, message, List.of(), Instant.now(), null);
	}

	public static ApiError of(String code, String message, List<String> details) {
		return new ApiError(code, message, details == null ? List.of() : details, Instant.now(), null);
	}

	public static ApiError rateLimited(String message, String limit) {
		return new ApiError("REPORT_RATE_LIMITED", message, List.of(), Instant.now(), limit);
	}
}
