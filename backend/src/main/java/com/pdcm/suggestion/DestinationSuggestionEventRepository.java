package com.pdcm.suggestion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DestinationSuggestionEventRepository extends JpaRepository<DestinationSuggestionEvent, Long> {

	@Query("""
			SELECT e FROM DestinationSuggestionEvent e
			JOIN FETCH e.actor
			WHERE e.suggestion.id = :suggestionId
			ORDER BY e.createdAt DESC
			""")
	List<DestinationSuggestionEvent> findBySuggestionIdOrderByCreatedAtDesc(
			@Param("suggestionId") Long suggestionId);
}
