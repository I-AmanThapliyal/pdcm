package com.pdcm.forecast;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

	List<Holiday> findByHolidayDateBetween(LocalDate start, LocalDate end);
}
