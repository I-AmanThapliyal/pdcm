package com.pdcm.suggestion;

import java.time.Instant;

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
@Table(name = "destination_suggestion_events")
public class DestinationSuggestionEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "suggestion_id", nullable = false)
	private DestinationSuggestion suggestion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_user_id", nullable = false)
	private UserAccount actor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private SuggestionEventAction action;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", nullable = false, length = 32)
	private SuggestionStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 32)
	private SuggestionStatus toStatus;

	@Column(length = 500)
	private String note;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected DestinationSuggestionEvent() {
	}

	public DestinationSuggestionEvent(
			DestinationSuggestion suggestion,
			UserAccount actor,
			SuggestionEventAction action,
			SuggestionStatus fromStatus,
			SuggestionStatus toStatus,
			String note) {
		this.suggestion = suggestion;
		this.actor = actor;
		this.action = action;
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

	public DestinationSuggestion getSuggestion() {
		return suggestion;
	}

	public UserAccount getActor() {
		return actor;
	}

	public SuggestionEventAction getAction() {
		return action;
	}

	public SuggestionStatus getFromStatus() {
		return fromStatus;
	}

	public SuggestionStatus getToStatus() {
		return toStatus;
	}

	public String getNote() {
		return note;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
