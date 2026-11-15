package com.pdcm.crowd;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/destinations/{destinationId}/crowd")
public class CrowdController {

	private final CrowdStatusService crowdStatusService;

	public CrowdController(CrowdStatusService crowdStatusService) {
		this.crowdStatusService = crowdStatusService;
	}

	@GetMapping
	public CrowdSummary current(@PathVariable Long destinationId) {
		return crowdStatusService.current(destinationId);
	}
}
