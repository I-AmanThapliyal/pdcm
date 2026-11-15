package com.pdcm.moderation;

import java.time.Instant;

import com.pdcm.report.VerificationStatus;
import com.pdcm.report.VisitReport;
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
@Table(name = "report_moderation_events")
public class ReportModerationEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "report_id", nullable = false)
	private VisitReport report;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_user_id", nullable = false)
	private UserAccount actor;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", nullable = false, length = 32)
	private VerificationStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 32)
	private VerificationStatus toStatus;

	@Column(length = 500)
	private String note;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ReportModerationEvent() {
	}

	public ReportModerationEvent(
			VisitReport report,
			UserAccount actor,
			VerificationStatus fromStatus,
			VerificationStatus toStatus,
			String note) {
		this.report = report;
		this.actor = actor;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.note = note;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public VisitReport getReport() {
		return report;
	}

	public UserAccount getActor() {
		return actor;
	}

	public VerificationStatus getFromStatus() {
		return fromStatus;
	}

	public VerificationStatus getToStatus() {
		return toStatus;
	}

	public String getNote() {
		return note;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
