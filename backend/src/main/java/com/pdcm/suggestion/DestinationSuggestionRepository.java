package com.pdcm.suggestion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DestinationSuggestionRepository extends JpaRepository<DestinationSuggestion, Long> {

	@Query(value = "SELECT pg_advisory_xact_lock(hashtext(:lockKey))", nativeQuery = true)
	Object acquireSuggestionLock(@Param("lockKey") String lockKey);

	List<DestinationSuggestion> findBySubmitterIdOrderByCreatedAtDesc(Long submitterId);

	List<DestinationSuggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status, Pageable pageable);

	@Query("""
			SELECT s FROM DestinationSuggestion s
			JOIN FETCH s.submitter
			LEFT JOIN FETCH s.createdDestination
			WHERE s.id = :id
			""")
	Optional<DestinationSuggestion> findByIdWithDetails(@Param("id") Long id);

	@Query("""
			SELECT s FROM DestinationSuggestion s
			JOIN FETCH s.submitter
			LEFT JOIN FETCH s.createdDestination
			WHERE s.status = :status
			ORDER BY s.createdAt DESC
			""")
	List<DestinationSuggestion> findQueue(@Param("status") SuggestionStatus status, Pageable pageable);
}
