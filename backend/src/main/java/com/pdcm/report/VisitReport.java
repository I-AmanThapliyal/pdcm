package com.pdcm.report;

import java.time.Instant;

import com.pdcm.destination.Destination;
import com.pdcm.user.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "visit_reports")
public class VisitReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "destination_id", nullable = false)
	private Destination destination;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserAccount user;

	@Column(name = "crowd_intensity", nullable = false)
	private int crowdIntensity;

	@Enumerated(EnumType.STRING)
	@Column(name = "traffic_condition", nullable = false, length = 20)
	private TrafficCondition trafficCondition;

	@Column(name = "wait_minutes")
	private Integer waitMinutes;

	@Column(length = 500)
	private String note;

	@Column(name = "observed_at", nullable = false)
	private Instant observedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "verification_status", nullable = false, length = 32)
	private VerificationStatus verificationStatus = VerificationStatus.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(name = "verification_reason", length = 40)
	private VerificationReason verificationReason;

	/** Private — never expose on public APIs. */
	@Column(name = "client_latitude")
	private Double clientLatitude;

	/** Private — never expose on public APIs. */
	@Column(name = "client_longitude")
	private Double clientLongitude;

	@Column(name = "client_accuracy_m")
	private Double clientAccuracyMeters;

	@Column(name = "distance_to_destination_m")
	private Double distanceToDestinationMeters;

	@Column(name = "location_consent_at")
	private Instant locationConsentAt;

	@Column(name = "location_purged_at")
	private Instant locationPurgedAt;

	protected VisitReport() {
	}

	public VisitReport(
			Destination destination,
			UserAccount user,
			int crowdIntensity,
			TrafficCondition trafficCondition,
			Integer waitMinutes,
			String note,
			Instant observedAt) {
		this.destination = destination;
		this.user = user;
		this.crowdIntensity = crowdIntensity;
		this.trafficCondition = trafficCondition;
		this.waitMinutes = waitMinutes;
		this.note = note;
		this.observedAt = observedAt;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (verificationStatus == null) {
			verificationStatus = VerificationStatus.PENDING;
		}
	}

	public void applyVerification(LocationVerificationResult result) {
		this.verificationStatus = result.status();
		this.verificationReason = result.reason();
		this.clientLatitude = result.clientLatitude();
		this.clientLongitude = result.clientLongitude();
		this.clientAccuracyMeters = result.clientAccuracyMeters();
		this.distanceToDestinationMeters = result.distanceToDestinationMeters();
		this.locationConsentAt = result.locationConsentAt();
	}

	/**
	 * Clears raw client coordinates and accuracy. Preserves verification status,
	 * reason, distance, and consent timestamp. Idempotent when already clear.
	 *
	 * @return true if coordinates were cleared on this call
	 */
	public boolean purgeClientCoordinates(Instant purgedAt) {
		boolean hadCoordinates = hasRetainedClientCoordinates();
		if (!hadCoordinates) {
			return false;
		}
		clientLatitude = null;
		clientLongitude = null;
		clientAccuracyMeters = null;
		if (locationPurgedAt == null) {
			locationPurgedAt = purgedAt;
		}
		return true;
	}

	public boolean hasRetainedClientCoordinates() {
		return clientLatitude != null || clientLongitude != null || clientAccuracyMeters != null;
	}

	public Long getId() {
		return id;
	}

	public Destination getDestination() {
		return destination;
	}

	public UserAccount getUser() {
		return user;
	}

	public int getCrowdIntensity() {
		return crowdIntensity;
	}

	public TrafficCondition getTrafficCondition() {
		return trafficCondition;
	}

	public Integer getWaitMinutes() {
		return waitMinutes;
	}

	public String getNote() {
		return note;
	}

	public Instant getObservedAt() {
		return observedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public VerificationStatus getVerificationStatus() {
		return verificationStatus;
	}

	public void setVerificationStatus(VerificationStatus verificationStatus) {
		this.verificationStatus = verificationStatus;
	}

	public VerificationReason getVerificationReason() {
		return verificationReason;
	}

	public void setVerificationReason(VerificationReason verificationReason) {
		this.verificationReason = verificationReason;
	}

	public Double getClientLatitude() {
		return clientLatitude;
	}

	public Double getClientLongitude() {
		return clientLongitude;
	}

	public Double getClientAccuracyMeters() {
		return clientAccuracyMeters;
	}

	public Double getDistanceToDestinationMeters() {
		return distanceToDestinationMeters;
	}

	public Instant getLocationConsentAt() {
		return locationConsentAt;
	}

	public Instant getLocationPurgedAt() {
		return locationPurgedAt;
	}
}
