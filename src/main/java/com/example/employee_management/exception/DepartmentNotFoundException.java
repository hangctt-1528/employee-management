package com.example.employee_management.exception;

// Thrown by EmployeeService when the departmentId in the request body does not exist;
// GlobalExceptionHandler answers 400 (the bad value is in the body, not in the URL).
public class DepartmentNotFoundException extends RuntimeException {

	public DepartmentNotFoundException(Long departmentId) {
		super("Department not found with id " + departmentId);
	}

}
