package com.pdcm.externaltraffic;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/destinations/{destinationId}/traffic")
public class ExternalTrafficController {

	private final ExternalTrafficService externalTrafficService;

	public ExternalTrafficController(ExternalTrafficService externalTrafficService) {
		this.externalTrafficService = externalTrafficService;
	}

	@GetMapping("/external")
	public ExternalTrafficResponse external(@PathVariable Long destinationId) {
		return externalTrafficService.current(destinationId);
	}
}
