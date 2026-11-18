package com.example.employee_management.dto;

// JSON body of POST / PUT /api/employees,
// e.g. {"name": "nguyen van an", "email": "an@example.com", "departmentId": 1}
public class EmployeeRequest {

	private String name;
	private String email;
	private Long departmentId;

	// Jackson needs the empty constructor and the setters to read the JSON body
	public EmployeeRequest() {
	}

	public EmployeeRequest(String name, String email, Long departmentId) {
		this.name = name;
		this.email = email;
		this.departmentId = departmentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Long getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(Long departmentId) {
		this.departmentId = departmentId;
	}

}
