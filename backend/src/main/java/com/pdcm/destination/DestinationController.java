package com.pdcm.destination;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pdcm.common.PageResponse;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

	private final DestinationService destinationService;

	public DestinationController(DestinationService destinationService) {
		this.destinationService = destinationService;
	}

	@GetMapping
	public PageResponse<DestinationResponse> list(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String region,
			@RequestParam(required = false) DestinationCategory category,
			@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
		return PageResponse.from(destinationService.search(q, region, category, pageable));
	}

	@GetMapping("/map")
	public List<DestinationResponse> map(
			@RequestParam double south,
			@RequestParam double west,
			@RequestParam double north,
			@RequestParam double east,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String region,
			@RequestParam(required = false) DestinationCategory category,
			@RequestParam(defaultValue = "100") int limit) {
		return destinationService.findInMapBounds(south, west, north, east, q, region, category, limit);
	}

	@GetMapping("/{id}")
	public DestinationResponse get(@PathVariable Long id) {
		return destinationService.getById(id);
	}

	@GetMapping("/{id}/nearby")
	public List<NearbyDestinationResponse> nearby(
			@PathVariable Long id,
			@RequestParam(defaultValue = "100") double radiusKm,
			@RequestParam(defaultValue = "10") int limit) {
		return destinationService.findNearby(id, radiusKm, limit);
	}
}
