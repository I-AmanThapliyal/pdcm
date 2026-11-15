package com.pdcm.common;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiError.of("NOT_FOUND", ex.getMessage()));
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of("CONFLICT", ex.getMessage()));
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiError.of("UNAUTHORIZED", ex.getMessage()));
	}

	@ExceptionHandler(RateLimitException.class)
	public ResponseEntity<ApiError> handleRateLimit(RateLimitException ex) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.body(ApiError.rateLimited(ex.getMessage(), ex.getLimit()));
	}

	@ExceptionHandler({
			MethodArgumentNotValidException.class,
			MethodArgumentTypeMismatchException.class,
			HttpMessageNotReadableException.class,
			IllegalArgumentException.class
	})
	public ResponseEntity<ApiError> handleValidation(Exception ex) {
		List<String> details = List.of();
		if (ex instanceof MethodArgumentNotValidException manv) {
			details = manv.getBindingResult().getFieldErrors().stream()
					.map(err -> err.getField() + ": " + err.getDefaultMessage())
					.toList();
		}
		else if (ex instanceof HttpMessageNotReadableException) {
			details = List.of("Request body is invalid or has the wrong type");
		}
		else if (ex.getMessage() != null) {
			details = List.of(ex.getMessage());
		}
		return ResponseEntity.badRequest()
				.body(ApiError.of("VALIDATION_ERROR", "Invalid request", details));
	}
}
