package com.pdcm.externaltraffic;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADMIN-only usage counters and remaining free-tier budget.
 * SecurityConfig already gates /api/admin/**.
 */
@RestController
@RequestMapping("/api/admin/providers/traffic")
public class AdminProviderTrafficController {

	private final ExternalTrafficService externalTrafficService;

	public AdminProviderTrafficController(ExternalTrafficService externalTrafficService) {
		this.externalTrafficService = externalTrafficService;
	}

	@GetMapping("/usage")
	public ProviderStatusResponse usage() {
		return externalTrafficService.providerStatus();
	}
}
