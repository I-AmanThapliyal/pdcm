package com.pdcm.dashboard;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationRepository;
import com.pdcm.forecast.IndiaCalendar;
import com.pdcm.report.TrafficCondition;
import com.pdcm.report.VerificationReason;
import com.pdcm.report.VerificationStatus;
import com.pdcm.report.VisitReport;
import com.pdcm.report.VisitReportRepository;
import com.pdcm.support.PostgresTestcontainers;
import com.pdcm.user.Role;
import com.pdcm.user.UserAccount;
import com.pdcm.user.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
class DashboardInformationQualityIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = PostgresTestcontainers.create();

	@DynamicPropertySource
	static void datasourceProps(DynamicPropertyRegistry registry) {
		PostgresTestcontainers.register(registry, postgres);
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	DestinationRepository destinationRepository;

	@Autowired
	VisitReportRepository visitReportRepository;

	@Autowired
	UserAccountRepository userAccountRepository;

	@Test
	void dashboardExposesCoverageProviderWindowAndHonestUnknownCrowd() throws Exception {
		mockMvc.perform(get("/api/dashboard").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.asOf").exists())
				.andExpect(jsonPath("$.observationWindowHours").value(6))
				.andExpect(jsonPath("$.catalogueCoverage.activeDestinationCount").isNumber())
				.andExpect(jsonPath("$.catalogueCoverage.regionCount").isNumber())
				.andExpect(jsonPath("$.catalogueCoverage.regions").isArray())
				.andExpect(jsonPath("$.externalTrafficProvider.provider").exists())
				.andExpect(jsonPath("$.externalTrafficProvider.configured").isBoolean())
				.andExpect(jsonPath("$.externalTrafficProvider.status").exists())
				.andExpect(jsonPath("$.popularDestinations.length()").value(6))
				.andExpect(jsonPath("$.popularDestinations[*].crowd.status", everyItem(not(nullValue()))))
				.andExpect(jsonPath("$.popularDestinations[*].crowd.source", everyItem(org.hamcrest.Matchers.is("USER_REPORTS"))))
				.andExpect(jsonPath("$.popularDestinations[*].traffic.source", everyItem(org.hamcrest.Matchers.is("USER_REPORTS"))))
				.andExpect(jsonPath("$.popularDestinations[*].forecastQuality.forecastStatus").exists())
				.andExpect(jsonPath("$.popularDestinations[*].forecastQuality.estimatedIntensity",
						everyItem(nullValue())))
				.andExpect(jsonPath("$.quieterKnownDestinations").isArray())
				.andExpect(jsonPath("$.calendarHighlight.calendarSignals").exists());
	}

	@Test
	void noReportsMeansUnknownCrowdNotQuietAndInsufficientForecast() throws Exception {
		mockMvc.perform(get("/api/dashboard").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.popularDestinations[?(@.crowd.status == 'QUIET')]").doesNotExist())
				.andExpect(jsonPath("$.popularDestinations[0].crowd.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.popularDestinations[0].crowd.confidence").value("NONE"))
				.andExpect(jsonPath("$.popularDestinations[0].crowd.contributingReports").value(0))
				.andExpect(jsonPath("$.popularDestinations[0].crowd.lastObservedAt").isEmpty())
				.andExpect(jsonPath("$.popularDestinations[0].traffic.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.popularDestinations[0].forecastQuality.forecastStatus")
						.value("INSUFFICIENT_DATA"))
				.andExpect(jsonPath("$.popularDestinations[0].forecastQuality.estimatedIntensity").isEmpty())
				.andExpect(jsonPath("$.quieterKnownDestinations.length()").value(0));
	}

	@Test
	void staleVerifiedReportOutsideWindowStaysUnknown() throws Exception {
		Destination destination = destinationRepository.findAll().get(8);
		UserAccount user = userAccountRepository.save(new UserAccount(
				"stale-dash@example.com", "Stale", "{noop}", Role.USER));
		VisitReport stale = new VisitReport(
				destination,
				user,
				1,
				TrafficCondition.LIGHT,
				null,
				null,
				Instant.now().minusSeconds(7 * 3600));
		stale.setVerificationStatus(VerificationStatus.LOCATION_VERIFIED);
		stale.setVerificationReason(VerificationReason.WITHIN_GEOFENCE);
		visitReportRepository.save(stale);

		mockMvc.perform(get("/api/dashboard").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.popularDestinations[?(@.destination.id == " + destination.getId()
						+ ")].crowd.status", hasItem("UNKNOWN")))
				.andExpect(jsonPath("$.popularDestinations[?(@.destination.id == " + destination.getId()
						+ ")].crowd.lastObservedAt", everyItem(nullValue())));
	}

	@Test
	void freshVerifiedReportSurfacesSourceConfidenceAndFreshnessTimestamp() throws Exception {
		Destination destination = destinationRepository.findAll().get(9);
		UserAccount user = userAccountRepository.save(new UserAccount(
				"fresh-dash@example.com", "Fresh", "{noop}", Role.USER));
		Instant observedAt = Instant.now().minusSeconds(900);
		VisitReport fresh = new VisitReport(
				destination,
				user,
				4,
				TrafficCondition.MODERATE,
				null,
				null,
				observedAt);
		fresh.setVerificationStatus(VerificationStatus.LOCATION_VERIFIED);
		fresh.setVerificationReason(VerificationReason.WITHIN_GEOFENCE);
		visitReportRepository.save(fresh);

		mockMvc.perform(get("/api/dashboard").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.popularDestinations[?(@.destination.id == " + destination.getId()
						+ ")].crowd.status", hasItem("CROWDED")))
				.andExpect(jsonPath("$.popularDestinations[?(@.destination.id == " + destination.getId()
						+ ")].crowd.source", hasItem("USER_REPORTS")))
				.andExpect(jsonPath("$.popularDestinations[?(@.destination.id == " + destination.getId()
						+ ")].crowd.confidence", hasItem("LOW")))
				.andExpect(jsonPath("$.popularDestinations[?(@.destination.id == " + destination.getId()
						+ ")].crowd.contributingReports", hasItem(1)))
				.andExpect(jsonPath("$.popularDestinations[?(@.destination.id == " + destination.getId()
						+ ")].crowd.lastObservedAt", everyItem(not(nullValue()))))
				.andExpect(jsonPath("$.popularDestinations[?(@.destination.id == " + destination.getId()
						+ ")].traffic.status", hasItem("MODERATE")))
				.andExpect(jsonPath("$.quieterKnownDestinations[?(@.crowd.status == 'UNKNOWN')]").doesNotExist());
	}

	@Test
	void sufficientHistoryYieldsEstimateWithoutTreatingItAsLive() throws Exception {
		Destination destination = destinationRepository.findAll().get(10);
		LocalDate tomorrow = LocalDate.now(IndiaCalendar.INDIA).plusDays(1);
		for (int week = 1; week <= 5; week++) {
			UserAccount user = userAccountRepository.save(new UserAccount(
					"hist-dash-" + week + "@example.com", "Hist" + week, "{noop}", Role.USER));
			VisitReport report = new VisitReport(
					destination,
					user,
					2,
					TrafficCondition.LIGHT,
					null,
					null,
					tomorrow.minusWeeks(week).atTime(11, 0).atZone(IndiaCalendar.INDIA).toInstant());
			report.setVerificationStatus(VerificationStatus.LOCATION_VERIFIED);
			report.setVerificationReason(VerificationReason.WITHIN_GEOFENCE);
			visitReportRepository.save(report);
		}

		mockMvc.perform(get("/api/dashboard").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.popularDestinations[?(@.destination.id == " + destination.getId()
						+ ")].forecastQuality.forecastStatus", hasItem("ESTIMATE")))
				.andExpect(jsonPath("$.popularDestinations[?(@.destination.id == " + destination.getId()
						+ ")].forecastQuality.estimatedIntensity", hasItem(2.0)))
				.andExpect(jsonPath("$.popularDestinations[?(@.destination.id == " + destination.getId()
						+ ")].forecastQuality.sampleSize", hasItem(5)))
				.andExpect(jsonPath("$.popularDestinations[?(@.destination.id == " + destination.getId()
						+ ")].crowd.status", hasItem("UNKNOWN")));
	}

	@Test
	void alternativesExposeFreshnessFieldsAndDoNotRankUnknownAsQuiet() throws Exception {
		Long hawaMahalId = destinationRepository.findAll().stream()
				.filter(destination -> "hawa-mahal".equals(destination.getSlug()))
				.findFirst()
				.orElseThrow()
				.getId();

		mockMvc.perform(get("/api/destinations/{id}/alternatives", hawaMahalId)
						.param("radiusKm", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].informationQuality").value("INSUFFICIENT_DATA"))
				.andExpect(jsonPath("$[0].crowdStatus").value("UNKNOWN"))
				.andExpect(jsonPath("$[0].contributingReports").value(0))
				.andExpect(jsonPath("$[0].lastObservedAt").isEmpty())
				.andExpect(jsonPath("$[0].source").value("USER_REPORTS"))
				.andExpect(jsonPath("$[?(@.crowdStatus == 'QUIET')]").doesNotExist());
	}
}
