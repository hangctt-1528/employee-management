package com.example.employee_management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// JSON body of POST / PUT /api/employees, e.g. {"name": "nguyen van an", "email": "an@example.com", "departmentId": 1},
// and also the form object of /employees/add (Lab 6).
// The constraints are checked by @Valid in EmployeeController (Lab 5) and EmployeeWebController (Lab 6).
public class EmployeeRequest {

	// @NotBlank rejects null, "" and "   " (@NotNull would only reject null)
	@NotBlank(message = "Name must not be blank")
	@Size(max = 100, message = "Name must be at most 100 characters")
	private String name;

	@NotBlank(message = "Email must not be blank")
	@Email(message = "Email is not valid")
	@Size(max = 100, message = "Email must be at most 100 characters")
	private String email;

	@NotNull(message = "Department is required")
	private Long departmentId;

	// Jackson (JSON body) and data binding (form fields) both create the object with
	// the empty constructor and fill it with the setters
	public EmployeeRequest() {
	}

	public EmployeeRequest(String name, String email, Long departmentId) {
		this.name = name;
		setEmail(email);
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

	// Jackson calls this setter while reading the JSON, so " an@example.com " is trimmed
	// before @Valid runs, and @Email checks "an@example.com"
	public void setEmail(String email) {
		if (email == null) {
			this.email = null;
		} else {
			this.email = email.trim();
		}
	}

	public Long getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(Long departmentId) {
		this.departmentId = departmentId;
	}

}
