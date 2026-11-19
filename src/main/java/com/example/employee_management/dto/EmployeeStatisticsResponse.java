package com.example.employee_management.dto;

import java.time.LocalDateTime;
import java.util.List;

// For /api/reports/statistics and /employees/statistics
public class EmployeeStatisticsResponse {

	private long totalEmployees;
	private List<DepartmentEmployeeCount> departments;
	private LocalDateTime generatedAt;

	public EmployeeStatisticsResponse() {
	}

	public EmployeeStatisticsResponse(long totalEmployees, List<DepartmentEmployeeCount> departments,
			LocalDateTime generatedAt) {
		this.totalEmployees = totalEmployees;
		this.departments = departments;
		this.generatedAt = generatedAt;
	}

	public long getTotalEmployees() {
		return totalEmployees;
	}

	public void setTotalEmployees(long totalEmployees) {
		this.totalEmployees = totalEmployees;
	}

	public List<DepartmentEmployeeCount> getDepartments() {
		return departments;
	}

	public void setDepartments(List<DepartmentEmployeeCount> departments) {
		this.departments = departments;
	}

	public LocalDateTime getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(LocalDateTime generatedAt) {
		this.generatedAt = generatedAt;
	}

}
