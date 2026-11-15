package com.pdcm.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

	@Test
	void haversine_samePoint_isZero() {
		assertThat(GeoUtils.haversineKm(28.61, 77.23, 28.61, 77.23)).isZero();
	}

	@Test
	void haversine_delhiToAgra_isAbout200Km() {
		// India Gate -> Taj Mahal (approx)
		double km = GeoUtils.haversineKm(28.6129, 77.2295, 27.1751, 78.0421);
		assertThat(km).isCloseTo(180.0, within(30.0));
	}

	@Test
	void haversine_jaipurCluster_areClose() {
		double km = GeoUtils.haversineKm(26.9255, 75.8236, 26.9239, 75.8267);
		assertThat(km).isLessThan(1.0);
	}
}
