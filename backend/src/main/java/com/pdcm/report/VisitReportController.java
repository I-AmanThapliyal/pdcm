package com.pdcm.report;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/destinations/{destinationId}/reports")
public class VisitReportController {

	private final VisitReportService visitReportService;

	public VisitReportController(VisitReportService visitReportService) {
		this.visitReportService = visitReportService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public VisitReportResponse submit(
			@PathVariable Long destinationId,
			@Valid @RequestBody CreateVisitReportRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return visitReportService.submit(destinationId, Long.valueOf(jwt.getSubject()), request);
	}
}
