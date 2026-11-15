package com.pdcm.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.pdcm.support.PostgresTestcontainers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthIntegrationTest {

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
	UserAccountRepository userAccountRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void register_hashesPassword_andReturnsToken() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"alice@example.com","displayName":"Alice","password":"password123"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.user.email").value("alice@example.com"))
				.andExpect(jsonPath("$.user.role").value("USER"))
				.andExpect(jsonPath("$.user.passwordHash").doesNotExist())
				.andReturn();

		UserAccount saved = userAccountRepository.findByEmailIgnoreCase("alice@example.com").orElseThrow();
		assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
		assertThat(passwordEncoder.matches("password123", saved.getPasswordHash())).isTrue();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		String token = body.get("accessToken").asText();

		mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("alice@example.com"));
	}

	@Test
	void register_duplicateEmail_returns409() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"dup@example.com","displayName":"One","password":"password123"}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"dup@example.com","displayName":"Two","password":"password123"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONFLICT"));
	}

	@Test
	void login_success_and_invalidCredentials() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"bob@example.com","displayName":"Bob","password":"password123"}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"bob@example.com","password":"password123"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"bob@example.com","password":"wrong-password"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"missing@example.com","password":"password123"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void me_requiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized());
	}
}
