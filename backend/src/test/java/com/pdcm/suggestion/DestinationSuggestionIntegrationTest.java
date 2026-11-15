package com.pdcm.suggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.pdcm.destination.Destination;
import com.pdcm.destination.DestinationRepository;
import com.pdcm.report.VisitReportRepository;
import com.pdcm.security.JwtService;
import com.pdcm.support.PostgresTestcontainers;
import com.pdcm.user.Role;
import com.pdcm.user.UserAccount;
import com.pdcm.user.UserAccountRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class DestinationSuggestionIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = PostgresTestcontainers.create();

	@DynamicPropertySource
	static void datasourceProps(DynamicPropertyRegistry registry) {
		PostgresTestcontainers.register(registry, postgres);
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	DestinationRepository destinationRepository;

	@Autowired
	VisitReportRepository visitReportRepository;

	@Autowired
	DestinationSuggestionRepository suggestionRepository;

	@Autowired
	DestinationSuggestionEventRepository eventRepository;

	@Autowired
	UserAccountRepository userAccountRepository;

	@Autowired
	JwtService jwtService;

	@Test
	void anonymousCannotSuggest() throws Exception {
		mockMvc.perform(post("/api/destination-suggestions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(uniqueSuggestionJson("Anon Spot", 12.97, 77.59)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void nonAdminCannotAccessSuggestionAdminApis() throws Exception {
		String userToken = register("suggest-user-forbid@example.com");

		mockMvc.perform(get("/api/admin/destination-suggestions")
						.header("Authorization", "Bearer " + userToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/admin/destination-suggestions/1/approve")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/admin/destination-suggestions/1/coordinates")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"latitude":28.54,"longitude":77.1}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void validationRejectsIncompleteAndOutOfBoxCoords() throws Exception {
		String userToken = register("suggest-validate@example.com");

		mockMvc.perform(post("/api/destination-suggestions")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"","city":"Pune","region":"Maharashtra","category":"NATURE","description":"x"}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/destination-suggestions")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Far Away",
								  "city":"Nowhere",
								  "region":"Maharashtra",
								  "category":"NATURE",
								  "description":"Outside India soft box",
								  "latitude":1.0,
								  "longitude":1.0
								}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/destination-suggestions")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Half Coords",
								  "city":"Pune",
								  "region":"Maharashtra",
								  "category":"CITY",
								  "description":"Only lat",
								  "latitude":18.52
								}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void userCanSubmitAndListOwnSuggestions_withDuplicateHints() throws Exception {
		Destination existing = destinationRepository.findBySlugAndActiveTrue("taj-mahal").orElseThrow();
		String userToken = register("suggest-owner@example.com");

		MvcResult created = mockMvc.perform(post("/api/destination-suggestions")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Taj Mahal",
								  "city":"Agra",
								  "region":"Uttar Pradesh",
								  "category":"HISTORICAL",
								  "description":"Possible duplicate of the famous mausoleum",
								  "latitude":%s,
								  "longitude":%s,
								  "referenceUrl":"https://example.com/taj"
								}
								""".formatted(existing.getLatitude(), existing.getLongitude())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUBMITTED"))
				.andExpect(jsonPath("$.possibleDuplicate").value(true))
				.andExpect(jsonPath("$.possibleDuplicates[0].destinationId").value(existing.getId().intValue()))
				.andExpect(jsonPath("$.contributorUserId").value(nullValue()))
				.andExpect(jsonPath("$.email").doesNotExist())
				.andReturn();

		long suggestionId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

		mockMvc.perform(get("/api/me/destination-suggestions")
						.header("Authorization", "Bearer " + userToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == %d)].status".formatted(suggestionId), hasItem("SUBMITTED")))
				.andExpect(jsonPath("$[0].email").doesNotExist());
	}

	@Test
	void approveCreatesDestinationWithUnknownCrowd_andNoVisitReports() throws Exception {
		String userToken = register("suggest-approve-user@example.com");
		UserAccount admin = userAccountRepository.save(new UserAccount(
				"suggest-approve-admin@example.com", "Admin", "{noop}", Role.ADMIN));
		String adminToken = jwtService.createAccessToken(admin);

		long beforeDestinations = destinationRepository.count();
		long beforeReports = visitReportRepository.count();

		MvcResult created = mockMvc.perform(post("/api/destination-suggestions")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(uniqueSuggestionJson("Quiet Hill Overlook", 30.4562, 78.0781)))
				.andExpect(status().isOk())
				.andReturn();
		long suggestionId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

		mockMvc.perform(get("/api/admin/destination-suggestions")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == %d)].contributorUserId".formatted(suggestionId)).exists())
				.andExpect(jsonPath("$[?(@.id == %d)].email".formatted(suggestionId)).doesNotExist());

		MvcResult approved = mockMvc.perform(post("/api/admin/destination-suggestions/{id}/approve", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"note":"Looks legitimate"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.createdDestinationId").isNumber())
				.andExpect(jsonPath("$.auditHistory[0].action").value("APPROVED"))
				.andExpect(jsonPath("$.auditHistory[0].actorUserId").value(admin.getId().intValue()))
				.andReturn();

		long destinationId = objectMapper.readTree(approved.getResponse().getContentAsString())
				.get("createdDestinationId")
				.asLong();

		assertThat(destinationRepository.count()).isEqualTo(beforeDestinations + 1);
		assertThat(visitReportRepository.count()).isEqualTo(beforeReports);

		Destination createdDestination = destinationRepository.findById(destinationId).orElseThrow();
		assertThat(createdDestination.getSourceReference()).isEqualTo("USER_SUGGESTION");
		assertThat(createdDestination.getSourceLicense()).isEqualTo("UNKNOWN");
		assertThat(createdDestination.isActive()).isTrue();

		mockMvc.perform(get("/api/destinations/{id}/crowd", destinationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.contributingReports").value(0));

		mockMvc.perform(get("/api/destinations/{id}/traffic", destinationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.contributingReports").value(0));

		mockMvc.perform(post("/api/admin/destination-suggestions/{id}/approve", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isConflict());
	}

	@Test
	void rejectRequiresPersistsAndExposesReason_withoutTouchingCatalogue() throws Exception {
		String userToken = register("suggest-reject-user@example.com");
		String otherToken = register("suggest-reject-other@example.com");
		UserAccount admin = userAccountRepository.save(new UserAccount(
				"suggest-reject-admin@example.com", "Admin", "{noop}", Role.ADMIN));
		String adminToken = jwtService.createAccessToken(admin);
		long beforeDestinations = destinationRepository.count();

		MvcResult created = mockMvc.perform(post("/api/destination-suggestions")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(uniqueSuggestionJson("Rejected Cove", 15.3, 73.9)))
				.andExpect(status().isOk())
				.andReturn();
		long suggestionId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

		mockMvc.perform(post("/api/admin/destination-suggestions/{id}/reject", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/admin/destination-suggestions/{id}/reject", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"note":"   "}
								"""))
				.andExpect(status().isBadRequest());

		String tooLong = "x".repeat(501);
		mockMvc.perform(post("/api/admin/destination-suggestions/{id}/reject", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"note":"%s"}
								""".formatted(tooLong)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/admin/destination-suggestions")
						.header("Authorization", "Bearer " + adminToken)
						.param("status", "SUBMITTED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == %d)]".formatted(suggestionId)).exists());

		mockMvc.perform(post("/api/admin/destination-suggestions/{id}/reject", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"note":"Insufficient evidence"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.createdDestinationId").value(nullValue()))
				.andExpect(jsonPath("$.adminNote").value("Insufficient evidence"))
				.andExpect(jsonPath("$.auditHistory[0].action").value("REJECTED"))
				.andExpect(jsonPath("$.auditHistory[0].actorUserId").value(admin.getId().intValue()))
				.andExpect(jsonPath("$.auditHistory[0].note").value("Insufficient evidence"));

		assertThat(destinationRepository.count()).isEqualTo(beforeDestinations);
		assertThat(suggestionRepository.findById(suggestionId).orElseThrow().getStatus())
				.isEqualTo(SuggestionStatus.REJECTED);
		assertThat(suggestionRepository.findById(suggestionId).orElseThrow().getAdminNote())
				.isEqualTo("Insufficient evidence");
		assertThat(eventRepository.findBySuggestionIdOrderByCreatedAtDesc(suggestionId))
				.filteredOn(event -> event.getAction() == SuggestionEventAction.REJECTED)
				.first()
				.satisfies(event -> {
					assertThat(event.getActor().getId()).isEqualTo(admin.getId());
					assertThat(event.getNote()).isEqualTo("Insufficient evidence");
					assertThat(event.getCreatedAt()).isNotNull();
				});

		mockMvc.perform(get("/api/admin/destination-suggestions")
						.header("Authorization", "Bearer " + adminToken)
						.param("status", "SUBMITTED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == %d)]".formatted(suggestionId)).isEmpty());

		mockMvc.perform(get("/api/admin/destination-suggestions")
						.header("Authorization", "Bearer " + adminToken)
						.param("status", "REJECTED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == %d)].adminNote".formatted(suggestionId), hasItem("Insufficient evidence")));

		mockMvc.perform(get("/api/me/destination-suggestions")
						.header("Authorization", "Bearer " + userToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == %d)].status".formatted(suggestionId), hasItem("REJECTED")))
				.andExpect(jsonPath("$[?(@.id == %d)].adminNote".formatted(suggestionId), hasItem("Insufficient evidence")));

		mockMvc.perform(get("/api/me/destination-suggestions/{id}", suggestionId)
						.header("Authorization", "Bearer " + userToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.adminNote").value("Insufficient evidence"))
				.andExpect(jsonPath("$.email").doesNotExist());

		mockMvc.perform(get("/api/me/destination-suggestions/{id}", suggestionId)
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/admin/destination-suggestions/{id}", suggestionId)
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void rejectDuplicateDoesNotAlterApprovedCatalogueDestination() throws Exception {
		String userToken = register("suggest-dup-reject-user@example.com");
		UserAccount admin = userAccountRepository.save(new UserAccount(
				"suggest-dup-reject-admin@example.com", "Admin", "{noop}", Role.ADMIN));
		String adminToken = jwtService.createAccessToken(admin);

		MvcResult first = mockMvc.perform(post("/api/destination-suggestions")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Ambience Mall Feedback",
								  "city":"Gurugram",
								  "region":"Haryana",
								  "category":"CITY",
								  "description":"First suggestion that becomes catalogue",
								  "latitude":28.541,
								  "longitude":77.098
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();
		long approvedSuggestionId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asLong();

		MvcResult approved = mockMvc.perform(post("/api/admin/destination-suggestions/{id}/approve", approvedSuggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"note":"Approved after review"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.createdDestinationId").isNumber())
				.andReturn();
		long catalogueId = objectMapper.readTree(approved.getResponse().getContentAsString())
				.get("createdDestinationId")
				.asLong();
		Destination catalogueBefore = destinationRepository.findById(catalogueId).orElseThrow();
		String nameBefore = catalogueBefore.getName();
		String slugBefore = catalogueBefore.getSlug();
		Double latBefore = catalogueBefore.getLatitude();
		Double lonBefore = catalogueBefore.getLongitude();
		long destinationCountBefore = destinationRepository.count();

		MvcResult second = mockMvc.perform(post("/api/destination-suggestions")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Ambience Mall Feedback",
								  "city":"Gurugram",
								  "region":"Haryana",
								  "category":"CITY",
								  "description":"Duplicate suggestion for rejection feedback",
								  "latitude":28.541,
								  "longitude":77.098
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.possibleDuplicate").value(true))
				.andExpect(jsonPath("$.possibleDuplicates[0].destinationId").value((int) catalogueId))
				.andReturn();
		long duplicateSuggestionId = objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asLong();

		String reason = "A destination with the same name and region already exists in our catalogue.";
		mockMvc.perform(post("/api/admin/destination-suggestions/{id}/reject", duplicateSuggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"note":"%s"}
								""".formatted(reason)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.createdDestinationId").value(nullValue()))
				.andExpect(jsonPath("$.adminNote").value(reason))
				.andExpect(jsonPath("$.possibleDuplicates[0].destinationId").value((int) catalogueId));

		assertThat(destinationRepository.count()).isEqualTo(destinationCountBefore);
		Destination catalogueAfter = destinationRepository.findById(catalogueId).orElseThrow();
		assertThat(catalogueAfter.getName()).isEqualTo(nameBefore);
		assertThat(catalogueAfter.getSlug()).isEqualTo(slugBefore);
		assertThat(catalogueAfter.getLatitude()).isEqualTo(latBefore);
		assertThat(catalogueAfter.getLongitude()).isEqualTo(lonBefore);
		assertThat(suggestionRepository.findById(approvedSuggestionId).orElseThrow().getStatus())
				.isEqualTo(SuggestionStatus.APPROVED);

		mockMvc.perform(get("/api/me/destination-suggestions/{id}", duplicateSuggestionId)
						.header("Authorization", "Bearer " + userToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.adminNote").value(reason))
				.andExpect(jsonPath("$.possibleDuplicates[0].destinationId").value((int) catalogueId));
	}

	@Test
	void concurrentApproveCreatesOnlyOneDestination() throws Exception {
		String userToken = register("suggest-race-user@example.com");
		UserAccount admin = userAccountRepository.save(new UserAccount(
				"suggest-race-admin@example.com", "Admin", "{noop}", Role.ADMIN));
		String adminToken = jwtService.createAccessToken(admin);

		MvcResult created = mockMvc.perform(post("/api/destination-suggestions")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(uniqueSuggestionJson("Race Ridge Point", 32.24, 77.19)))
				.andExpect(status().isOk())
				.andReturn();
		long suggestionId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
		long beforeDestinations = destinationRepository.count();

		ExecutorService pool = Executors.newFixedThreadPool(2);
		Callable<Integer> approveCall = () -> mockMvc.perform(
						post("/api/admin/destination-suggestions/{id}/approve", suggestionId)
								.header("Authorization", "Bearer " + adminToken)
								.contentType(MediaType.APPLICATION_JSON)
								.content("{}"))
				.andReturn()
				.getResponse()
				.getStatus();
		try {
			List<Future<Integer>> futures = pool.invokeAll(List.of(approveCall, approveCall));
			List<Integer> statuses = new ArrayList<>();
			for (Future<Integer> future : futures) {
				statuses.add(future.get(30, TimeUnit.SECONDS));
			}
			assertThat(statuses).contains(200);
			assertThat(statuses.stream().filter(code -> code == 200).count()).isEqualTo(1);
			assertThat(statuses.stream().filter(code -> code == 409).count()).isEqualTo(1);
		}
		finally {
			pool.shutdownNow();
		}

		assertThat(destinationRepository.count()).isEqualTo(beforeDestinations + 1);
		assertThat(suggestionRepository.findById(suggestionId).orElseThrow().getStatus())
				.isEqualTo(SuggestionStatus.APPROVED);
	}

	@Test
	void cannotApproveWithoutCoordinates() throws Exception {
		String userToken = register("suggest-nocoords-user@example.com");
		UserAccount admin = userAccountRepository.save(new UserAccount(
				"suggest-nocoords-admin@example.com", "Admin", "{noop}", Role.ADMIN));
		String adminToken = jwtService.createAccessToken(admin);

		MvcResult created = mockMvc.perform(post("/api/destination-suggestions")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"No Coords Mesa",
								  "city":"Leh",
								  "region":"Ladakh",
								  "category":"NATURE",
								  "description":"Suggested without map pin"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.latitude").value(nullValue()))
				.andReturn();
		long suggestionId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

		mockMvc.perform(post("/api/admin/destination-suggestions/{id}/approve", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isConflict());
	}

	@Test
	void adminCanSetCoordinatesThenApprove_andAuditRecordsUpdate() throws Exception {
		String userToken = register("suggest-coords-user@example.com");
		UserAccount admin = userAccountRepository.save(new UserAccount(
				"suggest-coords-admin@example.com", "Admin", "{noop}", Role.ADMIN));
		String adminToken = jwtService.createAccessToken(admin);

		MvcResult created = mockMvc.perform(post("/api/destination-suggestions")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Ambience Mall Coord Fix",
								  "city":"Gurugram",
								  "region":"Haryana",
								  "category":"CITY",
								  "description":"iMax"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.latitude").value(nullValue()))
				.andReturn();
		long suggestionId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

		mockMvc.perform(patch("/api/admin/destination-suggestions/{id}/coordinates", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"latitude":null,"longitude":77.1}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(patch("/api/admin/destination-suggestions/{id}/coordinates", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"latitude":91.0,"longitude":77.1}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(patch("/api/admin/destination-suggestions/{id}/coordinates", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"latitude":1.0,"longitude":1.0}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(patch("/api/admin/destination-suggestions/{id}/coordinates", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"latitude":28.541,"longitude":77.098}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.latitude").value(28.541))
				.andExpect(jsonPath("$.longitude").value(77.098))
				.andExpect(jsonPath("$.status").value("SUBMITTED"))
				.andExpect(jsonPath("$.auditHistory[0].action").value("COORDINATES_UPDATED"))
				.andExpect(jsonPath("$.auditHistory[0].actorUserId").value(admin.getId().intValue()))
				.andExpect(jsonPath("$.auditHistory[0].note").value("latitude=28.541, longitude=77.098"));

		mockMvc.perform(patch("/api/admin/destination-suggestions/{id}/coordinates", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"latitude":28.542,"longitude":77.099}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.latitude").value(28.542))
				.andExpect(jsonPath("$.longitude").value(77.099));

		mockMvc.perform(post("/api/admin/destination-suggestions/{id}/approve", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.createdDestinationId").isNumber());

		mockMvc.perform(patch("/api/admin/destination-suggestions/{id}/coordinates", suggestionId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"latitude":28.54,"longitude":77.1}
								"""))
				.andExpect(status().isConflict());
	}

	private String uniqueSuggestionJson(String name, double lat, double lon) {
		return """
				{
				  "name":"%s",
				  "city":"Testville",
				  "region":"Himachal Pradesh",
				  "category":"HILL_STATION",
				  "description":"A user-suggested place for Phase 5 coverage",
				  "latitude":%s,
				  "longitude":%s,
				  "referenceUrl":"https://example.com/ref"
				}
				""".formatted(name, lat, lon);
	}

	private String register(String email) throws Exception {
		String body = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","displayName":"Traveler","password":"password123"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode json = objectMapper.readTree(body);
		return json.get("accessToken").asText();
	}
}
