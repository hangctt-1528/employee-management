package com.example.employee_management.dto;

// JSON body of POST /api/employees, e.g. {"name": "nguyen van an", "email": "an@example.com"}
public class EmployeeRequest {

	private String name;
	private String email;

	// Jackson needs the empty constructor and the setters to read the JSON body
	public EmployeeRequest() {
	}

	public EmployeeRequest(String name, String email) {
		this.name = name;
		this.email = email;
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

}
