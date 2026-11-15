package com.pdcm.externaltraffic;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hard request budgets for external traffic. Consumption uses a Postgres advisory
 * transaction lock plus a conditional UPDATE so concurrent API instances cannot
 * exceed daily or monthly caps.
 */
@Service
public class ProviderBudgetService {

	private final ProviderRequestStatsRepository statsRepository;
	private final ExternalTrafficProperties properties;
	private final Clock clock;

	public ProviderBudgetService(
			ProviderRequestStatsRepository statsRepository,
			ExternalTrafficProperties properties,
			Clock clock) {
		this.statsRepository = statsRepository;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Atomically consume one Flow request against daily and monthly hard caps.
	 * Returns false when either budget is exhausted (no Pay As You Grow).
	 */
	@Transactional
	public boolean tryConsume(String provider) {
		LocalDate day = utcDay();
		YearMonth month = YearMonth.from(day);
		statsRepository.acquireBudgetLock(budgetLockKey(provider));
		statsRepository.ensureDayRow(provider, day);

		int updated = statsRepository.tryIncrementRequest(
				provider,
				day,
				properties.getDailyRequestBudget(),
				properties.getMonthlyRequestBudget(),
				month.atDay(1),
				month.atEndOfMonth());
		if (updated == 1) {
			return true;
		}
		statsRepository.incrementBudgetSkipped(provider, day);
		return false;
	}

	@Transactional
	public void recordSuccess(String provider) {
		LocalDate day = utcDay();
		statsRepository.acquireBudgetLock(budgetLockKey(provider));
		statsRepository.ensureDayRow(provider, day);
		statsRepository.incrementSuccess(provider, day);
	}

	@Transactional
	public void recordFailure(String provider) {
		LocalDate day = utcDay();
		statsRepository.acquireBudgetLock(budgetLockKey(provider));
		statsRepository.ensureDayRow(provider, day);
		statsRepository.incrementFailure(provider, day);
	}

	@Transactional(readOnly = true)
	public ProviderRequestStats snapshot(String provider) {
		LocalDate day = utcDay();
		return statsRepository.findByProviderAndDayUtc(provider, day)
				.orElseGet(() -> new ProviderRequestStats(provider, day));
	}

	@Transactional(readOnly = true)
	public int requestsThisMonth(String provider) {
		YearMonth month = YearMonth.from(utcDay());
		return (int) statsRepository.sumRequestCount(provider, month.atDay(1), month.atEndOfMonth());
	}

	@Transactional(readOnly = true)
	public int remainingDaily(String provider) {
		ProviderRequestStats stats = snapshot(provider);
		return Math.max(0, properties.getDailyRequestBudget() - stats.getRequestCount());
	}

	@Transactional(readOnly = true)
	public int remainingMonthly(String provider) {
		return Math.max(0, properties.getMonthlyRequestBudget() - requestsThisMonth(provider));
	}

	/** UTC calendar day used for daily budget buckets. */
	LocalDate utcDay() {
		return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
	}

	private static String budgetLockKey(String provider) {
		return "pdcm:traffic-budget:" + provider;
	}
}
