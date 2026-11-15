package com.pdcm.security;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.pdcm.user.UserAccount;

@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;

	public JwtService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
	}

	public String createAccessToken(UserAccount user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(jwtProperties.ttlMinutes() * 60);

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("pdcm-api")
				.issuedAt(now)
				.expiresAt(expiresAt)
				.subject(String.valueOf(user.getId()))
				.claim("email", user.getEmail())
				.claim("role", user.getRole().name())
				.claim("displayName", user.getDisplayName())
				.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
		return jwt.getTokenValue();
	}

	public long expiresInSeconds() {
		return jwtProperties.ttlMinutes() * 60;
	}
}
