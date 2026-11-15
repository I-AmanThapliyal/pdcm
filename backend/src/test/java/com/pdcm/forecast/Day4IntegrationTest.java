package com.pdcm.forecast;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationRepository;
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
class Day4IntegrationTest {

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
	void forecastWithoutHistory_returnsCalendarFactsAndInsufficientData() throws Exception {
		Long destinationId = destinationRepository.findAll().getFirst().getId();

		mockMvc.perform(get("/api/destinations/{id}/forecast", destinationId).param("date", "2026-12-25"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.targetDate").value("2026-12-25"))
				.andExpect(jsonPath("$.calendarSignals.isNationalHoliday").value(true))
				.andExpect(jsonPath("$.calendarSignals.holidayName").value("Christmas"))
				.andExpect(jsonPath("$.calendarSignals.isLongWeekend").value(true))
				.andExpect(jsonPath("$.crowdEstimate.forecastStatus").value("INSUFFICIENT_DATA"))
				.andExpect(jsonPath("$.crowdEstimate.estimatedIntensity").isEmpty())
				.andExpect(jsonPath("$.crowdEstimate.label").value("ESTIMATE_UNAVAILABLE"));
	}

	@Test
	void fiveHistoricalSameWeekdayReports_produceALabelledEstimate() throws Exception {
		Destination destination = destinationRepository.findAll().get(3);
		UserAccount user = userAccountRepository.save(new UserAccount(
				"history@example.com", "Historian", "{noop}", Role.USER));
		LocalDate saturday = LocalDate.now(IndiaCalendar.INDIA)
				.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
		for (int week = 1; week <= 5; week++) {
			VisitReport report = new VisitReport(
					destination,
					user,
					2,
					TrafficCondition.LIGHT,
					null,
					null,
					saturday.minusWeeks(week).atTime(11, 0).atZone(IndiaCalendar.INDIA).toInstant());
			report.setVerificationStatus(VerificationStatus.LOCATION_VERIFIED);
			report.setVerificationReason(VerificationReason.WITHIN_GEOFENCE);
			visitReportRepository.save(report);
		}

		mockMvc.perform(get("/api/destinations/{id}/forecast", destination.getId())
						.param("date", saturday.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.crowdEstimate.forecastStatus").value("ESTIMATE"))
				.andExpect(jsonPath("$.crowdEstimate.estimatedIntensity").value(2.0))
				.andExpect(jsonPath("$.crowdEstimate.sampleSize").value(5))
				.andExpect(jsonPath("$.crowdEstimate.label").value("ESTIMATE"))
				.andExpect(jsonPath("$.crowdEstimate.confidence").value("LOW"));
	}

	@Test
	void alternatives_doNotCallMissingDataQuiet() throws Exception {
		Long hawaMahalId = destinationRepository.findAll().stream()
				.filter(destination -> "hawa-mahal".equals(destination.getSlug()))
				.findFirst()
				.orElseThrow()
				.getId();

		mockMvc.perform(get("/api/destinations/{id}/alternatives", hawaMahalId)
						.param("radiusKm", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].destination.region").value("Rajasthan"))
				.andExpect(jsonPath("$[0].crowdStatus").value("UNKNOWN"))
				.andExpect(jsonPath("$[0].informationQuality").value("INSUFFICIENT_DATA"))
				.andExpect(jsonPath("$[?(@.crowdStatus == 'QUIET')]").doesNotExist());
	}

	@Test
	void dashboard_isPublic() throws Exception {
		mockMvc.perform(get("/api/dashboard").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.popularDestinations.length()").value(6))
				.andExpect(jsonPath("$.observationWindowHours").value(6))
				.andExpect(jsonPath("$.catalogueCoverage.activeDestinationCount").exists())
				.andExpect(jsonPath("$.externalTrafficProvider.status").exists())
				.andExpect(jsonPath("$.popularDestinations[0].forecastQuality.forecastStatus").exists())
				.andExpect(jsonPath("$.calendarHighlight.targetDate").exists())
				.andExpect(jsonPath("$.calendarHighlight.calendarSignals").exists())
				.andExpect(jsonPath("$.recentReports").isArray())
				.andExpect(jsonPath("$.quieterKnownDestinations").isArray());
	}

	@Test
	void registerStillWorksAlongsideNewReads() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"day4@example.com","displayName":"Day Four","password":"password123"}
								"""))
				.andExpect(status().isCreated());
	}
}
