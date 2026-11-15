package com.pdcm.dashboard;

import java.util.List;

/**
 * How much of the catalogue is active — not a crowd or traffic claim.
 */
public record CatalogueCoverage(
		long activeDestinationCount,
		int regionCount,
		List<String> regions
) {
}
