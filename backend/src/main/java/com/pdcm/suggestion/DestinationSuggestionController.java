package com.pdcm.suggestion;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/destination-suggestions")
public class DestinationSuggestionController {

	private final DestinationSuggestionService suggestionService;

	public DestinationSuggestionController(DestinationSuggestionService suggestionService) {
		this.suggestionService = suggestionService;
	}

	@PostMapping
	public DestinationSuggestionResponse submit(
			@Valid @RequestBody CreateDestinationSuggestionRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return suggestionService.submit(Long.valueOf(jwt.getSubject()), request);
	}
}
