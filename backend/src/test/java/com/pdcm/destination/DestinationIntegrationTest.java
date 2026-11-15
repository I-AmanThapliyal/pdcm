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
class DestinationIntegrationTest {

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
	void list_returnsSeededDestinations() throws Exception {
		mockMvc.perform(get("/api/destinations").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(100))
				.andExpect(jsonPath("$.content[0].name").exists())
				.andExpect(jsonPath("$.content[0].tags").isArray());
	}

	@Test
	void search_byRegionAndCategory_filters() throws Exception {
		mockMvc.perform(get("/api/destinations")
						.param("region", "Rajasthan")
						.param("category", "HISTORICAL")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(10));
	}

	@Test
	void search_byQuery_matchesName() throws Exception {
		mockMvc.perform(get("/api/destinations")
						.param("q", "taj")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].slug").value("taj-mahal"));
	}

	@Test
	void getById_returnsDetail() throws Exception {
		Long id = destinationRepository.findAll().stream()
				.filter(d -> "taj-mahal".equals(d.getSlug()))
				.findFirst()
				.orElseThrow()
				.getId();

		mockMvc.perform(get("/api/destinations/{id}", id).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slug").value("taj-mahal"))
				.andExpect(jsonPath("$.latitude").value(27.1751));
	}

	@Test
	void getById_unknown_returns404() throws Exception {
		mockMvc.perform(get("/api/destinations/{id}", 999999).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void nearby_jaipur_returnsLocalCluster() throws Exception {
		Long hawaMahalId = destinationRepository.findAll().stream()
				.filter(d -> "hawa-mahal".equals(d.getSlug()))
				.findFirst()
				.orElseThrow()
				.getId();

		mockMvc.perform(get("/api/destinations/{id}/nearby", hawaMahalId)
						.param("radiusKm", "20")
						.param("limit", "5")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].destination.region").value("Rajasthan"))
				.andExpect(jsonPath("$[0].distanceKm").exists());

		var body = mockMvc.perform(get("/api/destinations/{id}/nearby", hawaMahalId)
						.param("radiusKm", "20")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(body).contains("jaipur-city-palace");
		assertThat(body).contains("amber-fort");
	}
}
