package com.pdcm.forecast;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/destinations/{destinationId}/forecast")
public class ForecastController {

	private final ForecastService forecastService;

	public ForecastController(ForecastService forecastService) {
		this.forecastService = forecastService;
	}

	@GetMapping
	public ForecastResponse forecast(
			@PathVariable Long destinationId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return forecastService.forecast(destinationId, date);
	}
}
