package com.pdcm.externaltraffic;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "provider_request_stats",
		uniqueConstraints = @UniqueConstraint(name = "uq_provider_request_stats", columnNames = { "provider", "day_utc" }))
public class ProviderRequestStats {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 40)
	private String provider;

	@Column(name = "day_utc", nullable = false)
	private LocalDate dayUtc;

	@Column(name = "request_count", nullable = false)
	private int requestCount;

	@Column(name = "success_count", nullable = false)
	private int successCount;

	@Column(name = "failure_count", nullable = false)
	private int failureCount;

	@Column(name = "budget_skipped", nullable = false)
	private int budgetSkipped;

	protected ProviderRequestStats() {
	}

	public ProviderRequestStats(String provider, LocalDate dayUtc) {
		this.provider = provider;
		this.dayUtc = dayUtc;
	}

	public Long getId() {
		return id;
	}

	public String getProvider() {
		return provider;
	}

	public LocalDate getDayUtc() {
		return dayUtc;
	}

	public int getRequestCount() {
		return requestCount;
	}

	public int getSuccessCount() {
		return successCount;
	}

	public int getFailureCount() {
		return failureCount;
	}

	public int getBudgetSkipped() {
		return budgetSkipped;
	}

	public void incrementRequest() {
		requestCount++;
	}

	public void incrementSuccess() {
		successCount++;
	}

	public void incrementFailure() {
		failureCount++;
	}

	public void incrementBudgetSkipped() {
		budgetSkipped++;
	}
}
