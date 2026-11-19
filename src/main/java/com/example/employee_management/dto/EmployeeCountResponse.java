package com.example.employee_management.dto;

import java.time.LocalDateTime;

// JSON of GET /api/reports/employee-count, e.g. {"totalEmployees":5,"countedAt":"2026-11-19T10:15:30.123"}.
// The cache keeps this very object (not a copy) and returns it to every caller:
// do not call the setters on a value returned by ReportService, or the cached value changes too.
public class EmployeeCountResponse {

	private long totalEmployees;

	// When the database was counted. Two calls with the same countedAt = the second one came from the cache.
	private LocalDateTime countedAt;

	public EmployeeCountResponse() {
	}

	public EmployeeCountResponse(long totalEmployees, LocalDateTime countedAt) {
		this.totalEmployees = totalEmployees;
		this.countedAt = countedAt;
	}

	public long getTotalEmployees() {
		return totalEmployees;
	}

	public void setTotalEmployees(long totalEmployees) {
		this.totalEmployees = totalEmployees;
	}

	public LocalDateTime getCountedAt() {
		return countedAt;
	}

	public void setCountedAt(LocalDateTime countedAt) {
		this.countedAt = countedAt;
	}

}
