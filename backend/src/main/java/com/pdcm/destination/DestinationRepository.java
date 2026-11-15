package com.pdcm.destination;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DestinationRepository extends JpaRepository<Destination, Long> {

	Optional<Destination> findByIdAndActiveTrue(Long id);

	Optional<Destination> findBySlugAndActiveTrue(String slug);

	boolean existsBySlug(String slug);

	@Query("SELECT d FROM Destination d WHERE d.active = true")
	List<Destination> findAllActive();

	/**
	 * Callers must pass already-normalized filters:
	 * qPattern = "%" + lower(q) + "%" or null;
	 * regionLower = lower(region) or null.
	 * Avoid LOWER(:param) — Postgres can bind null params as bytea.
	 */
	@Query("""
			SELECT d FROM Destination d
			WHERE d.active = true
			  AND (:qPattern IS NULL OR LOWER(d.name) LIKE :qPattern
			       OR LOWER(d.description) LIKE :qPattern
			       OR LOWER(COALESCE(d.city, '')) LIKE :qPattern)
			  AND (:regionLower IS NULL OR LOWER(d.region) = :regionLower)
			  AND (:category IS NULL OR d.category = :category)
			""")
	Page<Destination> search(
			@Param("qPattern") String qPattern,
			@Param("regionLower") String regionLower,
			@Param("category") DestinationCategory category,
			Pageable pageable);

	@Query("""
			SELECT d FROM Destination d
			WHERE d.active = true
			  AND d.latitude BETWEEN :south AND :north
			  AND d.longitude BETWEEN :west AND :east
			  AND (:qPattern IS NULL OR LOWER(d.name) LIKE :qPattern
			       OR LOWER(d.description) LIKE :qPattern
			       OR LOWER(COALESCE(d.city, '')) LIKE :qPattern)
			  AND (:regionLower IS NULL OR LOWER(d.region) = :regionLower)
			  AND (:category IS NULL OR d.category = :category)
			ORDER BY d.name ASC
			""")
	List<Destination> findInBounds(
			@Param("south") double south,
			@Param("west") double west,
			@Param("north") double north,
			@Param("east") double east,
			@Param("qPattern") String qPattern,
			@Param("regionLower") String regionLower,
			@Param("category") DestinationCategory category,
			Pageable pageable);

	@Query("SELECT d FROM Destination d WHERE d.active = true AND d.id <> :id")
	List<Destination> findActiveExcluding(@Param("id") Long id);

	long countByActiveTrue();
}
