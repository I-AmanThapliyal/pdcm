package com.pdcm.suggestion;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdcm.common.ConflictException;
import com.pdcm.common.NotFoundException;
import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationRepository;
import com.pdcm.user.UserAccount;
import com.pdcm.user.UserAccountRepository;

@Service
public class DestinationSuggestionService {

	/** Soft India bounding box — same spirit as report verification; no paid geocoding. */
	static final double INDIA_LAT_MIN = 6.0;
	static final double INDIA_LAT_MAX = 37.5;
	static final double INDIA_LON_MIN = 68.0;
	static final double INDIA_LON_MAX = 98.0;

	private final DestinationSuggestionRepository suggestionRepository;
	private final DestinationSuggestionEventRepository eventRepository;
	private final DestinationRepository destinationRepository;
	private final UserAccountRepository userAccountRepository;
	private final DuplicateDestinationDetector duplicateDetector;

	public DestinationSuggestionService(
			DestinationSuggestionRepository suggestionRepository,
			DestinationSuggestionEventRepository eventRepository,
			DestinationRepository destinationRepository,
			UserAccountRepository userAccountRepository,
			DuplicateDestinationDetector duplicateDetector) {
		this.suggestionRepository = suggestionRepository;
		this.eventRepository = eventRepository;
		this.destinationRepository = destinationRepository;
		this.userAccountRepository = userAccountRepository;
		this.duplicateDetector = duplicateDetector;
	}

	@Transactional
	public DestinationSuggestionResponse submit(Long userId, CreateDestinationSuggestionRequest request) {
		UserAccount submitter = requireUser(userId);
		ValidatedFields fields = validateAndNormalize(request);

		DestinationSuggestion suggestion = new DestinationSuggestion(
				fields.name(),
				fields.city(),
				fields.region(),
				fields.category(),
				fields.description(),
				fields.latitude(),
				fields.longitude(),
				fields.referenceUrl(),
				submitter);
		suggestion = suggestionRepository.save(suggestion);

		eventRepository.save(new DestinationSuggestionEvent(
				suggestion,
				submitter,
				SuggestionEventAction.SUBMITTED,
				SuggestionStatus.SUBMITTED,
				SuggestionStatus.SUBMITTED,
				null));

		List<PossibleDuplicateResponse> duplicates = duplicateDetector.findPossibleDuplicates(
				fields.name(), fields.region(), fields.latitude(), fields.longitude());
		return DestinationSuggestionResponse.forOwner(suggestion, duplicates);
	}

	@Transactional(readOnly = true)
	public List<DestinationSuggestionResponse> listMine(Long userId) {
		return suggestionRepository.findBySubmitterIdOrderByCreatedAtDesc(userId).stream()
				.map(s -> DestinationSuggestionResponse.forOwner(s, duplicatesForReview(s)))
				.toList();
	}

	/**
	 * Owner-only detail. Non-owners get 404 (not 403) so suggestion ids are not enumerable.
	 */
	@Transactional(readOnly = true)
	public DestinationSuggestionResponse getMine(Long userId, Long suggestionId) {
		DestinationSuggestion suggestion = requireSuggestion(suggestionId);
		if (!suggestion.getSubmitter().getId().equals(userId)) {
			throw new NotFoundException("Destination suggestion not found: " + suggestionId);
		}
		return DestinationSuggestionResponse.forOwner(suggestion, duplicatesForReview(suggestion));
	}

	@Transactional(readOnly = true)
	public List<DestinationSuggestionResponse> listAdminQueue(SuggestionStatus status, int limit) {
		SuggestionStatus queue = status == null ? SuggestionStatus.SUBMITTED : status;
		if (queue != SuggestionStatus.SUBMITTED && queue != SuggestionStatus.REJECTED) {
			throw new IllegalArgumentException("Admin queue status must be SUBMITTED or REJECTED");
		}
		int pageSize = Math.clamp(limit, 1, 100);
		return suggestionRepository.findQueue(queue, PageRequest.of(0, pageSize)).stream()
				.map(s -> DestinationSuggestionResponse.forAdmin(s, duplicatesForReview(s), List.of()))
				.toList();
	}

	@Transactional(readOnly = true)
	public DestinationSuggestionResponse getAdminDetail(Long suggestionId) {
		DestinationSuggestion suggestion = requireSuggestion(suggestionId);
		List<SuggestionEventResponse> history = eventRepository
				.findBySuggestionIdOrderByCreatedAtDesc(suggestionId)
				.stream()
				.map(SuggestionEventResponse::from)
				.toList();
		return DestinationSuggestionResponse.forAdmin(suggestion, duplicatesForReview(suggestion), history);
	}

	@Transactional
	public DestinationSuggestionResponse updateCoordinates(
			Long suggestionId,
			Long adminUserId,
			UpdateSuggestionCoordinatesRequest request) {
		suggestionRepository.acquireSuggestionLock("pdcm:suggestion:" + suggestionId);
		DestinationSuggestion suggestion = requireSuggestion(suggestionId);
		UserAccount admin = requireUser(adminUserId);

		if (suggestion.getStatus() != SuggestionStatus.SUBMITTED) {
			throw new ConflictException(
					"Only SUBMITTED suggestions can have coordinates updated; current status is "
							+ suggestion.getStatus());
		}

		ValidatedCoordinates coords = requireValidCoordinates(request.latitude(), request.longitude());
		suggestion.updateCoordinates(coords.latitude(), coords.longitude());
		suggestionRepository.save(suggestion);

		String note = "latitude=" + coords.latitude() + ", longitude=" + coords.longitude();
		eventRepository.save(new DestinationSuggestionEvent(
				suggestion,
				admin,
				SuggestionEventAction.COORDINATES_UPDATED,
				SuggestionStatus.SUBMITTED,
				SuggestionStatus.SUBMITTED,
				note));

		List<PossibleDuplicateResponse> duplicates = duplicateDetector.findPossibleDuplicates(
				suggestion.getName(),
				suggestion.getRegion(),
				suggestion.getLatitude(),
				suggestion.getLongitude());
		List<SuggestionEventResponse> history = eventRepository
				.findBySuggestionIdOrderByCreatedAtDesc(suggestion.getId())
				.stream()
				.map(SuggestionEventResponse::from)
				.toList();
		return DestinationSuggestionResponse.forAdmin(suggestion, duplicates, history);
	}

	@Transactional
	public DestinationSuggestionResponse approve(Long suggestionId, Long adminUserId, SuggestionActionRequest request) {
		suggestionRepository.acquireSuggestionLock("pdcm:suggestion:" + suggestionId);
		DestinationSuggestion suggestion = requireSuggestion(suggestionId);
		UserAccount admin = requireUser(adminUserId);

		if (suggestion.getStatus() == SuggestionStatus.APPROVED) {
			throw new ConflictException("Suggestion is already approved");
		}
		if (suggestion.getStatus() != SuggestionStatus.SUBMITTED) {
			throw new ConflictException(
					"Only SUBMITTED suggestions can be approved; current status is " + suggestion.getStatus());
		}
		if (!suggestion.hasCoordinates()) {
			throw new ConflictException("Cannot approve a suggestion without latitude and longitude");
		}

		String note = normalizeNote(request);
		Destination destination = destinationRepository.save(new Destination(
				suggestion.getName(),
				allocateUniqueSlug(suggestion.getName()),
				suggestion.getDescription(),
				suggestion.getRegion(),
				suggestion.getCity(),
				suggestion.getCategory(),
				suggestion.getLatitude(),
				suggestion.getLongitude(),
				"USER_SUGGESTION",
				suggestion.getReferenceUrl(),
				"UNKNOWN"));

		SuggestionStatus from = suggestion.getStatus();
		suggestion.markApproved(destination, note);
		suggestionRepository.save(suggestion);

		eventRepository.save(new DestinationSuggestionEvent(
				suggestion,
				admin,
				SuggestionEventAction.APPROVED,
				from,
				SuggestionStatus.APPROVED,
				note));

		List<SuggestionEventResponse> history = eventRepository
				.findBySuggestionIdOrderByCreatedAtDesc(suggestion.getId())
				.stream()
				.map(SuggestionEventResponse::from)
				.toList();
		return DestinationSuggestionResponse.forAdmin(suggestion, List.of(), history);
	}

	@Transactional
	public DestinationSuggestionResponse reject(Long suggestionId, Long adminUserId, SuggestionActionRequest request) {
		suggestionRepository.acquireSuggestionLock("pdcm:suggestion:" + suggestionId);
		DestinationSuggestion suggestion = requireSuggestion(suggestionId);
		UserAccount admin = requireUser(adminUserId);

		if (suggestion.getStatus() == SuggestionStatus.REJECTED) {
			throw new ConflictException("Suggestion is already rejected");
		}
		if (suggestion.getStatus() != SuggestionStatus.SUBMITTED) {
			throw new ConflictException(
					"Only SUBMITTED suggestions can be rejected; current status is " + suggestion.getStatus());
		}

		String reason = requireRejectionReason(request);
		SuggestionStatus from = suggestion.getStatus();
		suggestion.markRejected(reason);
		suggestionRepository.save(suggestion);

		eventRepository.save(new DestinationSuggestionEvent(
				suggestion,
				admin,
				SuggestionEventAction.REJECTED,
				from,
				SuggestionStatus.REJECTED,
				reason));

		List<SuggestionEventResponse> history = eventRepository
				.findBySuggestionIdOrderByCreatedAtDesc(suggestion.getId())
				.stream()
				.map(SuggestionEventResponse::from)
				.toList();
		return DestinationSuggestionResponse.forAdmin(suggestion, duplicatesForReview(suggestion), history);
	}

	/** Duplicate hints stay available for SUBMITTED and REJECTED so contributors can open the catalogue match. */
	private List<PossibleDuplicateResponse> duplicatesForReview(DestinationSuggestion suggestion) {
		if (suggestion.getStatus() == SuggestionStatus.APPROVED) {
			return List.of();
		}
		return duplicateDetector.findPossibleDuplicates(
				suggestion.getName(),
				suggestion.getRegion(),
				suggestion.getLatitude(),
				suggestion.getLongitude());
	}

	private String allocateUniqueSlug(String name) {
		String base = SuggestionNameNormalizer.slugify(name);
		String candidate = base;
		int suffix = 2;
		while (destinationRepository.existsBySlug(candidate)) {
			String withSuffix = base + "-" + suffix;
			if (withSuffix.length() > 220) {
				int keep = Math.max(1, 220 - ("-" + suffix).length());
				withSuffix = base.substring(0, Math.min(base.length(), keep)) + "-" + suffix;
			}
			candidate = withSuffix;
			suffix++;
			if (suffix > 10_000) {
				throw new ConflictException("Unable to allocate a unique destination slug");
			}
		}
		return candidate;
	}

	private ValidatedFields validateAndNormalize(CreateDestinationSuggestionRequest request) {
		String name = requireTrimmed(request.name(), "name");
		String city = requireTrimmed(request.city(), "city");
		String region = requireTrimmed(request.region(), "region");
		String description = requireTrimmed(request.description(), "description");
		if (request.category() == null) {
			throw new IllegalArgumentException("category is required");
		}

		Double latitude = request.latitude();
		Double longitude = request.longitude();
		if ((latitude == null) != (longitude == null)) {
			throw new IllegalArgumentException("latitude and longitude must both be provided or both omitted");
		}
		if (latitude != null) {
			ValidatedCoordinates coords = requireValidCoordinates(latitude, longitude);
			latitude = coords.latitude();
			longitude = coords.longitude();
		}

		String referenceUrl = blankToNull(request.referenceUrl());
		if (referenceUrl != null) {
			referenceUrl = validateHttpUrl(referenceUrl);
		}

		return new ValidatedFields(
				name,
				city,
				region,
				request.category(),
				description,
				latitude,
				longitude,
				referenceUrl);
	}

	/**
	 * Required coords for admin update (and shared WGS84 / soft India rules for submit).
	 * Rejects null, out-of-range, null-island, and out-of-soft-India values.
	 */
	private static ValidatedCoordinates requireValidCoordinates(Double latitude, Double longitude) {
		if (latitude == null || longitude == null) {
			throw new IllegalArgumentException("latitude and longitude are required");
		}
		if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
			throw new IllegalArgumentException("latitude/longitude out of range");
		}
		if (isNullIsland(latitude, longitude)) {
			throw new IllegalArgumentException("coordinates look invalid (null island)");
		}
		if (!insideSoftIndiaBox(latitude, longitude)) {
			throw new IllegalArgumentException("coordinates must fall within the soft India bounding box");
		}
		return new ValidatedCoordinates(latitude, longitude);
	}

	private static String validateHttpUrl(String raw) {
		String value = raw.trim();
		try {
			URI uri = URI.create(value);
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			if (!"http".equals(scheme) && !"https".equals(scheme)) {
				throw new IllegalArgumentException("referenceUrl must be an http(s) URL");
			}
			if (uri.getHost() == null || uri.getHost().isBlank()) {
				throw new IllegalArgumentException("referenceUrl must include a host");
			}
			return value;
		}
		catch (IllegalArgumentException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("referenceUrl is not a valid URL");
		}
	}

	private static boolean insideSoftIndiaBox(double lat, double lon) {
		return lat >= INDIA_LAT_MIN && lat <= INDIA_LAT_MAX
				&& lon >= INDIA_LON_MIN && lon <= INDIA_LON_MAX;
	}

	private static boolean isNullIsland(double lat, double lon) {
		return Math.abs(lat) < 1e-6 && Math.abs(lon) < 1e-6;
	}

	private DestinationSuggestion requireSuggestion(Long id) {
		return suggestionRepository.findByIdWithDetails(id)
				.orElseThrow(() -> new NotFoundException("Destination suggestion not found: " + id));
	}

	private UserAccount requireUser(Long userId) {
		return userAccountRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found: " + userId));
	}

	private static String normalizeNote(SuggestionActionRequest request) {
		if (request == null || request.note() == null || request.note().isBlank()) {
			return null;
		}
		return request.note().trim();
	}

	/**
	 * Rejection always requires a non-blank reason (max 500 via {@link SuggestionActionRequest}).
	 * Blank or missing notes are rejected — never silently discarded.
	 */
	private static String requireRejectionReason(SuggestionActionRequest request) {
		if (request == null || request.note() == null || request.note().isBlank()) {
			throw new IllegalArgumentException("Rejection reason is required");
		}
		String reason = request.note().trim();
		if (reason.length() > 500) {
			throw new IllegalArgumentException("Rejection reason must be at most 500 characters");
		}
		return reason;
	}

	private static String requireTrimmed(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " is required");
		}
		return value.trim();
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private record ValidatedFields(
			String name,
			String city,
			String region,
			com.pdcm.destination.DestinationCategory category,
			String description,
			Double latitude,
			Double longitude,
			String referenceUrl) {
	}

	private record ValidatedCoordinates(double latitude, double longitude) {
	}
}
