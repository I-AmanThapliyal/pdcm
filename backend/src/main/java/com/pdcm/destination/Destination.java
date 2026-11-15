package com.pdcm.destination;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "destinations")
public class Destination {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, unique = true, length = 220)
	private String slug;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, length = 100)
	private String region;

	@Column(length = 120)
	private String city;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private DestinationCategory category;

	@Column(nullable = false)
	private Double latitude;

	@Column(nullable = false)
	private Double longitude;

	@Column(name = "crowd_sensitivity", length = 50)
	private String crowdSensitivity;

	@Column(name = "source_reference", length = 120)
	private String sourceReference;

	@Column(name = "source_url", length = 500)
	private String sourceUrl;

	@Column(name = "source_license", length = 80)
	private String sourceLicense;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "destination_tags", joinColumns = @JoinColumn(name = "destination_id"))
	@Column(name = "tag", nullable = false, length = 80)
	private Set<String> tags = new HashSet<>();

	protected Destination() {
		// JPA
	}

	/**
	 * Catalogue insert from an approved user suggestion. Crowd sensitivity stays
	 * unset (UNKNOWN until real reports). Never fabricate visit reports here.
	 */
	public Destination(
			String name,
			String slug,
			String description,
			String region,
			String city,
			DestinationCategory category,
			double latitude,
			double longitude,
			String sourceReference,
			String sourceUrl,
			String sourceLicense) {
		this.name = name;
		this.slug = slug;
		this.description = description;
		this.region = region;
		this.city = city;
		this.category = category;
		this.latitude = latitude;
		this.longitude = longitude;
		this.crowdSensitivity = null;
		this.sourceReference = sourceReference;
		this.sourceUrl = sourceUrl;
		this.sourceLicense = sourceLicense;
		this.active = true;
		this.tags = new HashSet<>();
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSlug() {
		return slug;
	}

	public String getDescription() {
		return description;
	}

	public String getRegion() {
		return region;
	}

	public String getCity() {
		return city;
	}

	public DestinationCategory getCategory() {
		return category;
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public String getCrowdSensitivity() {
		return crowdSensitivity;
	}

	public String getSourceReference() {
		return sourceReference;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public String getSourceLicense() {
		return sourceLicense;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Set<String> getTags() {
		return tags;
	}
}
