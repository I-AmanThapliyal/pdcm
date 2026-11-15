package com.pdcm.externaltraffic;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderRequestStatsRepository extends JpaRepository<ProviderRequestStats, Long> {

	Optional<ProviderRequestStats> findByProviderAndDayUtc(String provider, LocalDate dayUtc);

	@Query(value = "SELECT pg_advisory_xact_lock(hashtext(:lockKey))", nativeQuery = true)
	Object acquireBudgetLock(@Param("lockKey") String lockKey);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			INSERT INTO provider_request_stats
			    (provider, day_utc, request_count, success_count, failure_count, budget_skipped)
			VALUES (:provider, :dayUtc, 0, 0, 0, 0)
			ON CONFLICT (provider, day_utc) DO NOTHING
			""", nativeQuery = true)
	int ensureDayRow(@Param("provider") String provider, @Param("dayUtc") LocalDate dayUtc);

	/**
	 * Atomically increment request_count only when both daily and monthly caps allow it.
	 * Returns number of rows updated (1 = consumed, 0 = blocked).
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE provider_request_stats AS today
			SET request_count = today.request_count + 1
			WHERE today.provider = :provider
			  AND today.day_utc = :dayUtc
			  AND today.request_count < :dailyBudget
			  AND (
			        SELECT coalesce(sum(s.request_count), 0)
			        FROM provider_request_stats s
			        WHERE s.provider = :provider
			          AND s.day_utc BETWEEN :monthStart AND :monthEnd
			      ) < :monthlyBudget
			""", nativeQuery = true)
	int tryIncrementRequest(
			@Param("provider") String provider,
			@Param("dayUtc") LocalDate dayUtc,
			@Param("dailyBudget") int dailyBudget,
			@Param("monthlyBudget") int monthlyBudget,
			@Param("monthStart") LocalDate monthStart,
			@Param("monthEnd") LocalDate monthEnd);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE provider_request_stats
			SET budget_skipped = budget_skipped + 1
			WHERE provider = :provider AND day_utc = :dayUtc
			""", nativeQuery = true)
	int incrementBudgetSkipped(@Param("provider") String provider, @Param("dayUtc") LocalDate dayUtc);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE provider_request_stats
			SET success_count = success_count + 1
			WHERE provider = :provider AND day_utc = :dayUtc
			""", nativeQuery = true)
	int incrementSuccess(@Param("provider") String provider, @Param("dayUtc") LocalDate dayUtc);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE provider_request_stats
			SET failure_count = failure_count + 1
			WHERE provider = :provider AND day_utc = :dayUtc
			""", nativeQuery = true)
	int incrementFailure(@Param("provider") String provider, @Param("dayUtc") LocalDate dayUtc);

	@Query("""
			select coalesce(sum(s.requestCount), 0)
			from ProviderRequestStats s
			where s.provider = :provider and s.dayUtc between :start and :end
			""")
	long sumRequestCount(
			@Param("provider") String provider,
			@Param("start") LocalDate start,
			@Param("end") LocalDate end);
}
