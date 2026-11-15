package com.pdcm.traffic;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/destinations/{destinationId}/traffic")
public class TrafficController {

	private final TrafficStatusService trafficStatusService;

	public TrafficController(TrafficStatusService trafficStatusService) {
		this.trafficStatusService = trafficStatusService;
	}

	@GetMapping
	public TrafficSummary current(@PathVariable Long destinationId) {
		return trafficStatusService.current(destinationId);
	}
}