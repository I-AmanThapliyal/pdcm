package com.pdcm.report;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitReportRepository extends JpaRepository<VisitReport, Long> {

	@Query(value = "SELECT pg_advisory_xact_lock(hashtext(:lockKey))", nativeQuery = true)
	Object acquireUserReportLock(@Param("lockKey") String lockKey);

	long countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long userId, Instant startInclusive, Instant endExclusive);

	Optional<VisitReport> findFirstByUserIdAndDestinationIdOrderByCreatedAtDesc(Long userId, Long destinationId);

	@Query("""
			SELECT r FROM VisitReport r JOIN FETCH r.user
			WHERE r.destination.id = :destinationId
			  AND r.observedAt > :cutoff
			  AND r.verificationStatus = com.pdcm.report.VerificationStatus.LOCATION_VERIFIED
			""")
	java.util.List<VisitReport> findFreshByDestination(
			@Param("destinationId") Long destinationId,
			@Param("cutoff") Instant cutoff);

	/**
	 * All LOCATION_VERIFIED reports still inside the live freshness window (any destination).
	 * Used by dashboard to avoid per-destination N+1 aggregation queries.
	 */
	@Query("""
			SELECT r FROM VisitReport r
			JOIN FETCH r.user
			JOIN FETCH r.destination
			WHERE r.observedAt > :cutoff
			  AND r.verificationStatus = com.pdcm.report.VerificationStatus.LOCATION_VERIFIED
			""")
	java.util.List<VisitReport> findAllFresh(@Param("cutoff") Instant cutoff);

	@Query("""
			SELECT r FROM VisitReport r JOIN FETCH r.user
			WHERE r.destination.id = :destinationId
			  AND r.observedAt >= :cutoff
			  AND r.verificationStatus = com.pdcm.report.VerificationStatus.LOCATION_VERIFIED
			""")
	java.util.List<VisitReport> findHistory(
			@Param("destinationId") Long destinationId,
			@Param("cutoff") Instant cutoff);

	/**
	 * LOCATION_VERIFIED history for forecast batching across many destinations.
	 */
	@Query("""
			SELECT r FROM VisitReport r
			JOIN FETCH r.user
			JOIN FETCH r.destination
			WHERE r.observedAt >= :cutoff
			  AND r.verificationStatus = com.pdcm.report.VerificationStatus.LOCATION_VERIFIED
			""")
	java.util.List<VisitReport> findAllHistorySince(@Param("cutoff") Instant cutoff);

	@Query("""
			SELECT r.destination.id, COUNT(r) FROM VisitReport r
			WHERE r.createdAt >= :since
			GROUP BY r.destination.id
			""")
	java.util.List<Object[]> countByDestinationSince(@Param("since") Instant since);

	@Query("""
			SELECT r.destination.id, COUNT(r) FROM VisitReport r
			WHERE r.createdAt >= :since
			  AND r.verificationStatus = com.pdcm.report.VerificationStatus.LOCATION_VERIFIED
			GROUP BY r.destination.id
			""")
	java.util.List<Object[]> countVerifiedByDestinationSince(@Param("since") Instant since);

	java.util.List<VisitReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

	@Query("""
			SELECT r FROM VisitReport r
			JOIN FETCH r.destination
			JOIN FETCH r.user
			WHERE r.verificationStatus = :status
			ORDER BY r.createdAt DESC
			""")
	java.util.List<VisitReport> findByVerificationStatusOrderByCreatedAtDesc(
			@Param("status") VerificationStatus status,
			Pageable pageable);

	@Query("""
			SELECT r FROM VisitReport r
			JOIN FETCH r.destination
			JOIN FETCH r.user
			WHERE r.id = :id
			""")
	Optional<VisitReport> findByIdWithDestinationAndUser(@Param("id") Long id);

	/**
	 * Clears retained client coordinates older than the retention cutoff.
	 * Preserves verification metadata. Idempotent for already-cleared rows.
	 * Never selects or logs coordinate values.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE visit_reports
			SET client_latitude = NULL,
			    client_longitude = NULL,
			    client_accuracy_m = NULL,
			    location_purged_at = COALESCE(location_purged_at, :purgedAt)
			WHERE created_at < :cutoff
			  AND (client_latitude IS NOT NULL
			    OR client_longitude IS NOT NULL
			    OR client_accuracy_m IS NOT NULL)
			""", nativeQuery = true)
	int purgeClientCoordinatesOlderThan(
			@Param("cutoff") Instant cutoff,
			@Param("purgedAt") Instant purgedAt);
}
