package com.pdcm.recommendation;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/destinations/{destinationId}/alternatives")
public class RecommendationController {

	private final RecommendationService recommendationService;

	public RecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@GetMapping
	public List<AlternativeResponse> alternatives(
			@PathVariable Long destinationId,
			@RequestParam(defaultValue = "150") double radiusKm,
			@RequestParam(defaultValue = "5") int limit) {
		return recommendationService.alternatives(destinationId, radiusKm, limit);
	}
}