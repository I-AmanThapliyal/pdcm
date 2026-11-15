package com.pdcm.suggestion;

import java.time.Instant;

import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationCategory;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "destination_suggestions")
public class DestinationSuggestion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "normalized_name", nullable = false, length = 200)
	private String normalizedName;

	@Column(nullable = false, length = 120)
	private String city;

	@Column(nullable = false, length = 100)
	private String region;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private DestinationCategory category;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	private Double latitude;

	private Double longitude;

	@Column(name = "reference_url", length = 500)
	private String referenceUrl;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "submitter_user_id", nullable = false)
	private UserAccount submitter;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private SuggestionStatus status = SuggestionStatus.SUBMITTED;

	@Column(name = "admin_note", length = 500)
	private String adminNote;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_destination_id")
	private Destination createdDestination;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected DestinationSuggestion() {
	}

	public DestinationSuggestion(
			String name,
			String city,
			String region,
			DestinationCategory category,
			String description,
			Double latitude,
			Double longitude,
			String referenceUrl,
			UserAccount submitter) {
		this.name = name;
		this.normalizedName = SuggestionNameNormalizer.normalize(name);
		this.city = city;
		this.region = region;
		this.category = category;
		this.description = description;
		this.latitude = latitude;
		this.longitude = longitude;
		this.referenceUrl = referenceUrl;
		this.submitter = submitter;
		this.status = SuggestionStatus.SUBMITTED;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public void markApproved(Destination destination, String adminNote) {
		this.status = SuggestionStatus.APPROVED;
		this.createdDestination = destination;
		this.adminNote = adminNote;
	}

	public void markRejected(String adminNote) {
		this.status = SuggestionStatus.REJECTED;
		this.adminNote = adminNote;
	}

	public void updateCoordinates(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public boolean hasCoordinates() {
		return latitude != null && longitude != null;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

	public String getCity() {
		return city;
	}

	public String getRegion() {
		return region;
	}

	public DestinationCategory getCategory() {
		return category;
	}

	public String getDescription() {
		return description;
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public String getReferenceUrl() {
		return referenceUrl;
	}

	public UserAccount getSubmitter() {
		return submitter;
	}

	public SuggestionStatus getStatus() {
		return status;
	}

	public String getAdminNote() {
		return adminNote;
	}

	public Destination getCreatedDestination() {
		return createdDestination;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
