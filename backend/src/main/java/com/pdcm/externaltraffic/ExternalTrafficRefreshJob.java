package com.pdcm.externaltraffic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pdcm.destination.DestinationRepository;

@Component
@ConditionalOnProperty(prefix = "pdcm.external-traffic", name = "schedule-enabled", havingValue = "true")
public class ExternalTrafficRefreshJob {

	private static final Logger log = LoggerFactory.getLogger(ExternalTrafficRefreshJob.class);

	private final DestinationRepository destinationRepository;
	private final ExternalTrafficService externalTrafficService;
	private final ExternalTrafficProperties properties;

	public ExternalTrafficRefreshJob(
			DestinationRepository destinationRepository,
			ExternalTrafficService externalTrafficService,
			ExternalTrafficProperties properties) {
		this.destinationRepository = destinationRepository;
		this.externalTrafficService = externalTrafficService;
		this.properties = properties;
	}

	@Scheduled(fixedDelayString = "${pdcm.external-traffic.schedule-fixed-delay-ms:10800000}")
	public void refreshPilotDestinations() {
		if (!properties.hasApiKey()) {
			return;
		}
		for (String slug : properties.getPilotSlugs()) {
			destinationRepository.findBySlugAndActiveTrue(slug).ifPresentOrElse(
					destination -> {
						try {
							externalTrafficService.refreshPilotDestination(destination);
						} catch (Exception ex) {
							log.warn("Scheduled external traffic refresh failed for {}: {}", slug, ex.toString());
						}
					},
					() -> log.debug("Pilot slug not found: {}", slug));
		}
	}
}
