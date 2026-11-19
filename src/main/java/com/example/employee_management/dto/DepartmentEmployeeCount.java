package com.example.employee_management.dto;

// One statistics row, built by "select new" in DepartmentRepository
public class DepartmentEmployeeCount {

	private Long departmentId;
	private String departmentName;
	private Long employeeCount;

	public DepartmentEmployeeCount() {
	}

	// Types must match the query (count(...) is a Long)
	public DepartmentEmployeeCount(Long departmentId, String departmentName, Long employeeCount) {
		this.departmentId = departmentId;
		this.departmentName = departmentName;
		this.employeeCount = employeeCount;
	}

	public Long getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(Long departmentId) {
		this.departmentId = departmentId;
	}

	public String getDepartmentName() {
		return departmentName;
	}

	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}

	public Long getEmployeeCount() {
		return employeeCount;
	}

	public void setEmployeeCount(Long employeeCount) {
		this.employeeCount = employeeCount;
	}

}
