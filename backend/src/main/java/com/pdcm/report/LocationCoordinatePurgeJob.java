package com.pdcm.report;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pdcm.report-verification", name = "purge-enabled", havingValue = "true", matchIfMissing = true)
public class LocationCoordinatePurgeJob {

	private final LocationCoordinatePurgeService purgeService;

	public LocationCoordinatePurgeJob(LocationCoordinatePurgeService purgeService) {
		this.purgeService = purgeService;
	}

	@Scheduled(fixedDelayString = "${pdcm.report-verification.purge-fixed-delay-ms:300000}")
	public void run() {
		purgeService.purgeExpired();
	}
}
