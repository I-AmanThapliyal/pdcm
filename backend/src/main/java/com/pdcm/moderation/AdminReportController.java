package com.pdcm.moderation;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pdcm.report.VerificationStatus;

import jakarta.validation.Valid;

/**
 * ADMIN-only report moderation. SecurityConfig already gates /api/admin/**.
 * Raw coordinates are omitted unless includeCoordinates=true and still retained.
 * Callers must never log retained coordinates.
 */
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

	private final ReportModerationService reportModerationService;

	public AdminReportController(ReportModerationService reportModerationService) {
		this.reportModerationService = reportModerationService;
	}

	@GetMapping
	public List<AdminReportSummaryResponse> list(
			@RequestParam(defaultValue = "FLAGGED") VerificationStatus status,
			@RequestParam(defaultValue = "50") int limit) {
		return reportModerationService.listForReview(status, limit);
	}

	@GetMapping("/{id}")
	public AdminReportDetailResponse get(
			@PathVariable Long id,
			@RequestParam(defaultValue = "false") boolean includeCoordinates) {
		return reportModerationService.getReport(id, includeCoordinates);
	}

	@PostMapping("/{id}/verify")
	public AdminReportDetailResponse verify(
			@PathVariable Long id,
			@Valid @RequestBody(required = false) ModerationActionRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return reportModerationService.verify(id, Long.valueOf(jwt.getSubject()), noteOrEmpty(request));
	}

	@PostMapping("/{id}/reject")
	public AdminReportDetailResponse reject(
			@PathVariable Long id,
			@Valid @RequestBody(required = false) ModerationActionRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return reportModerationService.reject(id, Long.valueOf(jwt.getSubject()), noteOrEmpty(request));
	}

	@PostMapping("/{id}/flag")
	public AdminReportDetailResponse flag(
			@PathVariable Long id,
			@Valid @RequestBody(required = false) ModerationActionRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return reportModerationService.flag(id, Long.valueOf(jwt.getSubject()), noteOrEmpty(request));
	}

	private static ModerationActionRequest noteOrEmpty(ModerationActionRequest request) {
		return request == null ? new ModerationActionRequest(null) : request;
	}
}
