package com.pdcm.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pdcm.cors")
public record CorsProperties(
		List<String> allowedOrigins
) {
}
