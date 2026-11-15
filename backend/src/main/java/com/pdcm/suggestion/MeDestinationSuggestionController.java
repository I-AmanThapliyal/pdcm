package com.pdcm.suggestion;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/destination-suggestions")
public class MeDestinationSuggestionController {

	private final DestinationSuggestionService suggestionService;

	public MeDestinationSuggestionController(DestinationSuggestionService suggestionService) {
		this.suggestionService = suggestionService;
	}

	@GetMapping
	public List<DestinationSuggestionResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
		return suggestionService.listMine(Long.valueOf(jwt.getSubject()));
	}

	@GetMapping("/{id}")
	public DestinationSuggestionResponse getMine(
			@PathVariable Long id,
			@AuthenticationPrincipal Jwt jwt) {
		return suggestionService.getMine(Long.valueOf(jwt.getSubject()), id);
	}
}
