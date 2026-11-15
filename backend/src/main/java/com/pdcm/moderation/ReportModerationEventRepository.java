package com.pdcm.moderation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportModerationEventRepository extends JpaRepository<ReportModerationEvent, Long> {

	@Query("""
			SELECT e FROM ReportModerationEvent e
			JOIN FETCH e.actor
			WHERE e.report.id = :reportId
			ORDER BY e.createdAt DESC
			""")
	List<ReportModerationEvent> findByReportIdOrderByCreatedAtDesc(@Param("reportId") Long reportId);
}
