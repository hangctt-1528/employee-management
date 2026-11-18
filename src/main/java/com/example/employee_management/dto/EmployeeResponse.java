package com.example.employee_management.dto;

// JSON returned by the API. Jackson builds the JSON keys from the getters.
public class EmployeeResponse {

	private Long id;
	private String code;
	private String name;
	private String email;

	public EmployeeResponse() {
	}

	public EmployeeResponse(Long id, String code, String name, String email) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
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
