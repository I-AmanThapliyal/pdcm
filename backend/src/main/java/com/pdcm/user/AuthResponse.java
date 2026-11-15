package com.pdcm.user;

public record AuthResponse(
		String accessToken,
		String tokenType,
		long expiresInSeconds,
		UserResponse user
) {
	public static AuthResponse bearer(String token, long expiresInSeconds, UserAccount user) {
		return new AuthResponse(token, "Bearer", expiresInSeconds, UserResponse.from(user));
	}
}
