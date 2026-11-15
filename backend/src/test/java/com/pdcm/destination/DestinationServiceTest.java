package com.pdcm.destination;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pdcm.common.NotFoundException;

@ExtendWith(MockitoExtension.class)
class DestinationServiceTest {

	@Mock
	DestinationRepository destinationRepository;

	@InjectMocks
	DestinationService destinationService;

	@Test
	void getById_missing_throwsNotFound() {
		when(destinationRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> destinationService.getById(99L))
				.isInstanceOf(NotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void nearby_invalidRadius_throws() {
		assertThatThrownBy(() -> destinationService.findNearby(1L, 0, 5))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("radiusKm");
		assertThatThrownBy(() -> destinationService.findNearby(1L, 501, 5))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("radiusKm");
	}

	@Test
	void nearby_invalidLimit_throws() {
		assertThatThrownBy(() -> destinationService.findNearby(1L, 10, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("limit");
	}
}
