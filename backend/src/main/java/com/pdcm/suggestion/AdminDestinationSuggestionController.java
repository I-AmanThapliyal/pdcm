package com.pdcm.suggestion;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * ADMIN-only suggestion review. SecurityConfig already gates /api/admin/**.
 * Public surfaces never receive submitter email; admin sees contributorUserId only.
 */
@RestController
@RequestMapping("/api/admin/destination-suggestions")
public class AdminDestinationSuggestionController {

	private final DestinationSuggestionService suggestionService;

	public AdminDestinationSuggestionController(DestinationSuggestionService suggestionService) {
		this.suggestionService = suggestionService;
	}

	@GetMapping
	public List<DestinationSuggestionResponse> list(
			@RequestParam(defaultValue = "SUBMITTED") SuggestionStatus status,
			@RequestParam(defaultValue = "50") int limit) {
		return suggestionService.listAdminQueue(status, limit);
	}

	@GetMapping("/{id}")
	public DestinationSuggestionResponse get(@PathVariable Long id) {
		return suggestionService.getAdminDetail(id);
	}

	@PatchMapping("/{id}/coordinates")
	public DestinationSuggestionResponse updateCoordinates(
			@PathVariable Long id,
			@Valid @RequestBody UpdateSuggestionCoordinatesRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return suggestionService.updateCoordinates(id, Long.valueOf(jwt.getSubject()), request);
	}

	@PostMapping("/{id}/approve")
	public DestinationSuggestionResponse approve(
			@PathVariable Long id,
			@Valid @RequestBody(required = false) SuggestionActionRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return suggestionService.approve(id, Long.valueOf(jwt.getSubject()), noteOrEmpty(request));
	}

	@PostMapping("/{id}/reject")
	public DestinationSuggestionResponse reject(
			@PathVariable Long id,
			@Valid @RequestBody SuggestionActionRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return suggestionService.reject(id, Long.valueOf(jwt.getSubject()), request);
	}

	private static SuggestionActionRequest noteOrEmpty(SuggestionActionRequest request) {
		return request == null ? new SuggestionActionRequest(null) : request;
	}
}
