package com.pdcm.report;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdcm.common.NotFoundException;
import com.pdcm.common.RateLimitException;
import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationRepository;
import com.pdcm.user.UserAccount;
import com.pdcm.user.UserAccountRepository;

@Service
public class VisitReportService {

	public static final int DAILY_CAP = 10;
	public static final Duration COOLDOWN = Duration.ofMinutes(30);
	public static final Duration MAX_BACKDATE = Duration.ofHours(24);
	public static final Duration FUTURE_SKEW = Duration.ofSeconds(60);
	public static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

	private final VisitReportRepository visitReportRepository;
	private final DestinationRepository destinationRepository;
	private final UserAccountRepository userAccountRepository;
	private final ReportLocationVerifier reportLocationVerifier;
	private final Clock clock;

	public VisitReportService(
			VisitReportRepository visitReportRepository,
			DestinationRepository destinationRepository,
			UserAccountRepository userAccountRepository,
			ReportLocationVerifier reportLocationVerifier,
			Clock clock) {
		this.visitReportRepository = visitReportRepository;
		this.destinationRepository = destinationRepository;
		this.userAccountRepository = userAccountRepository;
		this.reportLocationVerifier = reportLocationVerifier;
		this.clock = clock;
	}

	@Transactional
	public VisitReportResponse submit(Long destinationId, Long userId, CreateVisitReportRequest request) {
		Instant now = clock.instant();
		validateObservedAt(request.observedAt(), now);

		Destination destination = destinationRepository.findByIdAndActiveTrue(destinationId)
				.orElseThrow(() -> new NotFoundException("Destination not found: " + destinationId));
		UserAccount user = userAccountRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found: " + userId));

		// Exactly one user-scoped advisory lock for the whole transaction.
		visitReportRepository.acquireUserReportLock("pdcm:report:" + userId);

		LocalDate indiaDay = now.atZone(INDIA).toLocalDate();
		ZonedDateTime startOfDayIndia = indiaDay.atStartOfDay(INDIA);
		Instant dayStart = startOfDayIndia.toInstant();
		Instant dayEnd = startOfDayIndia.plusDays(1).toInstant();

		long todayCount = visitReportRepository
				.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, dayStart, dayEnd);
		if (todayCount >= DAILY_CAP) {
			throw new RateLimitException(
					"Daily report limit reached (10 per India calendar day)",
					"DAILY_10");
		}

		visitReportRepository.findFirstByUserIdAndDestinationIdOrderByCreatedAtDesc(userId, destinationId)
				.ifPresent(latest -> {
					if (Duration.between(latest.getCreatedAt(), now).compareTo(COOLDOWN) < 0) {
						throw new RateLimitException(
								"Please wait 30 minutes before reporting this destination again",
								"COOLDOWN_30M");
					}
				});

		String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
		VisitReport report = new VisitReport(
				destination,
				user,
				request.crowdIntensity(),
				request.trafficCondition(),
				request.waitMinutes(),
				note,
				request.observedAt());
		report.applyVerification(reportLocationVerifier.verify(
				destination,
				request.locationConsent(),
				request.clientLocation()));

		return VisitReportResponse.from(visitReportRepository.save(report));
	}

	private static void validateObservedAt(Instant observedAt, Instant now) {
		if (observedAt.isAfter(now.plus(FUTURE_SKEW))) {
			throw new IllegalArgumentException("observedAt cannot be more than 60 seconds in the future");
		}
		if (observedAt.isBefore(now.minus(MAX_BACKDATE))) {
			throw new IllegalArgumentException("observedAt cannot be older than 24 hours");
		}
	}
}
