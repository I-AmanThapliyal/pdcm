package com.pdcm.externaltraffic;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "provider_health")
public class ProviderHealth {

	@Id
	@Column(length = 40)
	private String provider;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProviderHealthStatus status;

	@Column(name = "last_success_at")
	private Instant lastSuccessAt;

	@Column(name = "last_failure_at")
	private Instant lastFailureAt;

	@Column(name = "last_error_code", length = 80)
	private String lastErrorCode;

	@Column(name = "consecutive_failures", nullable = false)
	private int consecutiveFailures;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ProviderHealth() {
	}

	public ProviderHealth(String provider, ProviderHealthStatus status) {
		this.provider = provider;
		this.status = status;
		this.consecutiveFailures = 0;
		this.updatedAt = Instant.now();
	}

	public String getProvider() {
		return provider;
	}

	public ProviderHealthStatus getStatus() {
		return status;
	}

	public void setStatus(ProviderHealthStatus status) {
		this.status = status;
	}

	public Instant getLastSuccessAt() {
		return lastSuccessAt;
	}

	public void setLastSuccessAt(Instant lastSuccessAt) {
		this.lastSuccessAt = lastSuccessAt;
	}

	public Instant getLastFailureAt() {
		return lastFailureAt;
	}

	public void setLastFailureAt(Instant lastFailureAt) {
		this.lastFailureAt = lastFailureAt;
	}

	public String getLastErrorCode() {
		return lastErrorCode;
	}

	public void setLastErrorCode(String lastErrorCode) {
		this.lastErrorCode = lastErrorCode;
	}

	public int getConsecutiveFailures() {
		return consecutiveFailures;
	}

	public void setConsecutiveFailures(int consecutiveFailures) {
		this.consecutiveFailures = consecutiveFailures;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
