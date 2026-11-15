package com.pdcm.externaltraffic;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers/traffic")
public class ProviderStatusController {

	private final ExternalTrafficService externalTrafficService;

	public ProviderStatusController(ExternalTrafficService externalTrafficService) {
		this.externalTrafficService = externalTrafficService;
	}

	@GetMapping("/status")
	public ProviderStatusResponse status() {
		return externalTrafficService.providerStatus();
	}
}
