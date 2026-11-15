package com.pdcm.moderation;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdcm.common.ConflictException;
import com.pdcm.common.GeoUtils;
import com.pdcm.common.NotFoundException;
import com.pdcm.report.LocationCoordinatePurgeService;
import com.pdcm.report.ReportVerificationProperties;
import com.pdcm.report.VerificationReason;
import com.pdcm.report.VerificationStatus;
import com.pdcm.report.VisitReport;
import com.pdcm.report.VisitReportRepository;
import com.pdcm.user.UserAccount;
import com.pdcm.user.UserAccountRepository;

/**
 * ADMIN-only moderation. Never promotes LEGACY_UNVERIFIED. Never promotes
 * PENDING/FLAGGED to LOCATION_VERIFIED without an explicit verify action that
 * confirms location eligibility from retained distance/coords.
 */
@Service
public class ReportModerationService {

	private final VisitReportRepository visitReportRepository;
	private final ReportModerationEventRepository moderationEventRepository;
	private final UserAccountRepository userAccountRepository;
	private final ReportVerificationProperties verificationProperties;
	private final LocationCoordinatePurgeService purgeService;

	public ReportModerationService(
			VisitReportRepository visitReportRepository,
			ReportModerationEventRepository moderationEventRepository,
			UserAccountRepository userAccountRepository,
			ReportVerificationProperties verificationProperties,
			LocationCoordinatePurgeService purgeService) {
		this.visitReportRepository = visitReportRepository;
		this.moderationEventRepository = moderationEventRepository;
		this.userAccountRepository = userAccountRepository;
		this.verificationProperties = verificationProperties;
		this.purgeService = purgeService;
	}

	@Transactional(readOnly = true)
	public List<AdminReportSummaryResponse> listForReview(VerificationStatus status, int limit) {
		VerificationStatus queue = status == null ? VerificationStatus.FLAGGED : status;
		if (queue != VerificationStatus.FLAGGED && queue != VerificationStatus.PENDING) {
			throw new IllegalArgumentException("Review queue status must be FLAGGED or PENDING");
		}
		int pageSize = Math.clamp(limit, 1, 100);
		return visitReportRepository
				.findByVerificationStatusOrderByCreatedAtDesc(queue, PageRequest.of(0, pageSize))
				.stream()
				.map(AdminReportSummaryResponse::from)
				.toList();
	}

	@Transactional
	public AdminReportDetailResponse getReport(Long reportId, boolean includeCoordinates) {
		VisitReport report = requireReport(reportId);
		purgeService.purgeIfExpired(report);
		return toDetail(report, includeCoordinates);
	}

	/**
	 * Explicit verification: FLAGGED or PENDING → LOCATION_VERIFIED only when
	 * location eligibility can be confirmed from distance and/or retained coords.
	 * LEGACY_UNVERIFIED and REJECTED cannot be promoted this way.
	 */
	@Transactional
	public AdminReportDetailResponse verify(Long reportId, Long actorUserId, ModerationActionRequest request) {
		VisitReport report = requireReport(reportId);
		purgeService.purgeIfExpired(report);
		UserAccount actor = requireActor(actorUserId);

		VerificationStatus from = report.getVerificationStatus();
		if (from != VerificationStatus.FLAGGED && from != VerificationStatus.PENDING) {
			throw new ConflictException(
					"Only FLAGGED or PENDING reports can be verified; current status is " + from);
		}
		assertLocationEligible(report);

		return transition(
				report,
				actor,
				VerificationStatus.LOCATION_VERIFIED,
				VerificationReason.ADMIN_VERIFIED,
				normalizeNote(request));
	}

	@Transactional
	public AdminReportDetailResponse reject(Long reportId, Long actorUserId, ModerationActionRequest request) {
		VisitReport report = requireReport(reportId);
		purgeService.purgeIfExpired(report);
		UserAccount actor = requireActor(actorUserId);

		VerificationStatus from = report.getVerificationStatus();
		if (from != VerificationStatus.FLAGGED && from != VerificationStatus.LOCATION_VERIFIED) {
			throw new ConflictException(
					"Only FLAGGED or LOCATION_VERIFIED reports can be rejected; current status is " + from);
		}

		return transition(
				report,
				actor,
				VerificationStatus.REJECTED,
				VerificationReason.ADMIN_REJECTED,
				normalizeNote(request));
	}

	@Transactional
	public AdminReportDetailResponse flag(Long reportId, Long actorUserId, ModerationActionRequest request) {
		VisitReport report = requireReport(reportId);
		purgeService.purgeIfExpired(report);
		UserAccount actor = requireActor(actorUserId);

		VerificationStatus from = report.getVerificationStatus();
		if (from != VerificationStatus.LOCATION_VERIFIED) {
			throw new ConflictException(
					"Only LOCATION_VERIFIED reports can be flagged for abuse; current status is " + from);
		}

		return transition(
				report,
				actor,
				VerificationStatus.FLAGGED,
				VerificationReason.ADMIN_FLAGGED,
				normalizeNote(request));
	}

	private AdminReportDetailResponse transition(
			VisitReport report,
			UserAccount actor,
			VerificationStatus toStatus,
			VerificationReason reason,
			String note) {
		VerificationStatus from = report.getVerificationStatus();
		report.setVerificationStatus(toStatus);
		report.setVerificationReason(reason);
		visitReportRepository.save(report);

		ReportModerationEvent event = moderationEventRepository.save(
				new ReportModerationEvent(report, actor, from, toStatus, note));
		// Ensure report id is available on the event for response mapping.
		event.getReport().getId();

		return toDetail(report, false);
	}

	private void assertLocationEligible(VisitReport report) {
		Double distance = report.getDistanceToDestinationMeters();
		if (report.hasRetainedClientCoordinates()
				&& report.getClientLatitude() != null
				&& report.getClientLongitude() != null) {
			double lat = report.getClientLatitude();
			double lon = report.getClientLongitude();
			if (!insideSoftIndiaBox(lat, lon) || isNullIsland(lat, lon)) {
				throw new ConflictException(
						"Retained coordinates remain outside soft eligibility bounds; cannot verify");
			}
			distance = GeoUtils.haversineMeters(
					report.getDestination().getLatitude(),
					report.getDestination().getLongitude(),
					lat,
					lon);
		}

		if (distance == null) {
			throw new ConflictException(
					"Cannot verify without a calculated distance or retained eligible coordinates");
		}
		if (distance > verificationProperties.getGeofenceRadiusMeters()) {
			throw new ConflictException(
					"Distance exceeds configured geofence; cannot promote to LOCATION_VERIFIED");
		}
	}

	private boolean insideSoftIndiaBox(double lat, double lon) {
		return lat >= verificationProperties.getIndiaLatMin()
				&& lat <= verificationProperties.getIndiaLatMax()
				&& lon >= verificationProperties.getIndiaLonMin()
				&& lon <= verificationProperties.getIndiaLonMax();
	}

	private static boolean isNullIsland(double lat, double lon) {
		return Math.abs(lat) < 1e-6 && Math.abs(lon) < 1e-6;
	}

	private AdminReportDetailResponse toDetail(VisitReport report, boolean includeCoordinates) {
		AdminReportDetailResponse.AdminRetainedCoordinates coords = null;
		if (includeCoordinates && report.hasRetainedClientCoordinates()
				&& report.getClientLatitude() != null
				&& report.getClientLongitude() != null) {
			coords = new AdminReportDetailResponse.AdminRetainedCoordinates(
					report.getClientLatitude(),
					report.getClientLongitude(),
					report.getClientAccuracyMeters());
		}
		List<ModerationEventResponse> history = moderationEventRepository
				.findByReportIdOrderByCreatedAtDesc(report.getId())
				.stream()
				.map(ModerationEventResponse::from)
				.toList();
		return new AdminReportDetailResponse(AdminReportSummaryResponse.from(report), coords, history);
	}

	private VisitReport requireReport(Long reportId) {
		return visitReportRepository.findByIdWithDestinationAndUser(reportId)
				.orElseThrow(() -> new NotFoundException("Visit report not found: " + reportId));
	}

	private UserAccount requireActor(Long actorUserId) {
		return userAccountRepository.findById(actorUserId)
				.orElseThrow(() -> new NotFoundException("User not found: " + actorUserId));
	}

	private static String normalizeNote(ModerationActionRequest request) {
		if (request == null || request.note() == null || request.note().isBlank()) {
			return null;
		}
		return request.note().trim();
	}
}
