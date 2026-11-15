package com.pdcm.forecast;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "holidays")
public class Holiday {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "holiday_date", nullable = false, unique = true)
	private LocalDate holidayDate;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, length = 20)
	private String scope;

	protected Holiday() {
	}

	public LocalDate getHolidayDate() {
		return holidayDate;
	}

	public String getName() {
		return name;
	}

	public String getScope() {
		return scope;
	}
}
