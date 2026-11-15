package com.pdcm.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

public final class PostgresTestcontainers {

	private PostgresTestcontainers() {
	}

	@SuppressWarnings("resource")
	public static PostgreSQLContainer<?> create() {
		return new PostgreSQLContainer<>("postgres:16")
				.withDatabaseName("pdcm")
				.withUsername("pdcm")
				.withPassword("pdcm");
	}

	public static void register(DynamicPropertyRegistry registry, PostgreSQLContainer<?> postgres) {
		registry.add("spring.datasource.url",
				() -> postgres.getJdbcUrl() + (postgres.getJdbcUrl().contains("?") ? "&" : "?")
						+ "options=-c%20TimeZone=UTC");
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.hikari.connection-init-sql", () -> "SET TIME ZONE 'UTC'");
	}
}
