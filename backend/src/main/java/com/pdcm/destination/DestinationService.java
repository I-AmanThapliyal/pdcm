package com.pdcm.destination;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdcm.common.GeoUtils;
import com.pdcm.common.NotFoundException;

@Service
@Transactional(readOnly = true)
public class DestinationService {

	private final DestinationRepository destinationRepository;

	public DestinationService(DestinationRepository destinationRepository) {
		this.destinationRepository = destinationRepository;
	}

	public Page<DestinationResponse> search(String q, String region, DestinationCategory category, Pageable pageable) {
		String qPattern = blankToNull(q) == null ? null : "%" + blankToNull(q).toLowerCase() + "%";
		String regionLower = blankToNull(region) == null ? null : blankToNull(region).toLowerCase();
		return destinationRepository.search(qPattern, regionLower, category, pageable)
				.map(DestinationResponse::from);
	}

	public DestinationResponse getById(Long id) {
		return DestinationResponse.from(requireActive(id));
	}

	public List<DestinationResponse> findInMapBounds(
			double south,
			double west,
			double north,
			double east,
			String q,
			String region,
			DestinationCategory category,
			int limit) {
		MapBoundsRequest bounds = new MapBoundsRequest(south, west, north, east);
		bounds.validate();
		if (limit <= 0 || limit > MapBoundsRequest.MAX_LIMIT) {
			throw new IllegalArgumentException("limit must be between 1 and " + MapBoundsRequest.MAX_LIMIT);
		}
		String qPattern = blankToNull(q) == null ? null : "%" + blankToNull(q).toLowerCase() + "%";
		String regionLower = blankToNull(region) == null ? null : blankToNull(region).toLowerCase();
		return destinationRepository.findInBounds(
						south, west, north, east, qPattern, regionLower, category,
						PageRequest.of(0, limit))
				.stream()
				.map(DestinationResponse::from)
				.toList();
	}

	public static final double MAX_NEARBY_RADIUS_KM = 500.0;

	public List<NearbyDestinationResponse> findNearby(Long id, double radiusKm, int limit) {
		if (radiusKm <= 0 || radiusKm > MAX_NEARBY_RADIUS_KM) {
			throw new IllegalArgumentException("radiusKm must be between 0 (exclusive) and " + MAX_NEARBY_RADIUS_KM);
		}
		if (limit <= 0 || limit > 50) {
			throw new IllegalArgumentException("limit must be between 1 and 50");
		}

		Destination origin = requireActive(id);
		return destinationRepository.findActiveExcluding(id).stream()
				.map(candidate -> {
					double distance = GeoUtils.haversineKm(
							origin.getLatitude(), origin.getLongitude(),
							candidate.getLatitude(), candidate.getLongitude());
					return new NearbyDestinationResponse(DestinationResponse.from(candidate), round1(distance));
				})
				.filter(item -> item.distanceKm() <= radiusKm)
				.sorted(Comparator.comparingDouble(NearbyDestinationResponse::distanceKm))
				.limit(limit)
				.toList();
	}

	private Destination requireActive(Long id) {
		return destinationRepository.findByIdAndActiveTrue(id)
				.orElseThrow(() -> new NotFoundException("Destination not found: " + id));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static double round1(double value) {
		return Math.round(value * 10.0) / 10.0;
	}
}
