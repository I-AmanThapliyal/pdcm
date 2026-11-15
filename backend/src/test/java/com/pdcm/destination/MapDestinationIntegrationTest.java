package com.pdcm.destination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.pdcm.support.PostgresTestcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class MapDestinationIntegrationTest {

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

	@Test
	void catalogueHasOneHundredActiveDestinationsWithCityAndProvenance() {
		assertThat(destinationRepository.countByActiveTrue()).isEqualTo(100);
		assertThat(destinationRepository.findAll()).allSatisfy(destination -> {
			assertThat(destination.getCity()).isNotBlank();
			assertThat(destination.getSourceReference()).isNotBlank();
			assertThat(destination.getSourceLicense()).isNotBlank();
		});
		assertThat(destinationRepository.findAll().stream()
				.filter(destination -> "LEGACY_SEED".equals(destination.getSourceReference()))
				.count()).isEqualTo(20);
		assertThat(destinationRepository.findAll().stream()
				.filter(destination -> destination.getSourceReference().startsWith("Wikidata "))
				.count()).isEqualTo(80);
	}

	@Test
	void mapBounds_returnsJaipurCluster() throws Exception {
		String body = mockMvc.perform(get("/api/destinations/map")
						.param("south", "26.8")
						.param("west", "75.7")
						.param("north", "27.1")
						.param("east", "76.0")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].city").exists())
				.andExpect(jsonPath("$[0].sourceLicense").exists())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(body).contains("hawa-mahal");
		assertThat(body).contains("amber-fort");
	}

	@Test
	void mapBounds_supportsFiltersAndCap() throws Exception {
		String body = mockMvc.perform(get("/api/destinations/map")
						.param("south", "6")
						.param("west", "68")
						.param("north", "36")
						.param("east", "98")
						.param("region", "Rajasthan")
						.param("category", "HISTORICAL")
						.param("limit", "5")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].region").value("Rajasthan"))
				.andExpect(jsonPath("$[0].category").value("HISTORICAL"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(body.split("\"slug\"").length - 1).isLessThanOrEqualTo(5);
	}

	@Test
	void mapBounds_rejectsInvalidAndOversizeRequests() throws Exception {
		mockMvc.perform(get("/api/destinations/map")
						.param("south", "28")
						.param("west", "77")
						.param("north", "27")
						.param("east", "78")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(get("/api/destinations/map")
						.param("south", "6")
						.param("west", "68")
						.param("north", "36")
						.param("east", "98")
						.param("limit", "500")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	void destinationsWithoutReports_remainUnknownOnCrowdAndTraffic() throws Exception {
		Long id = destinationRepository.findAll().stream()
				.filter(destination -> "charminar".equals(destination.getSlug()))
				.findFirst()
				.orElseThrow()
				.getId();

		mockMvc.perform(get("/api/destinations/{id}/crowd", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.confidence").value("NONE"));

		mockMvc.perform(get("/api/destinations/{id}/traffic", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.confidence").value("NONE"));
	}
}
