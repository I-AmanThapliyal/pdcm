package com.pdcm.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pdcm.security.jwt")
public record JwtProperties(
		String secret,
		long ttlMinutes
) {
}
