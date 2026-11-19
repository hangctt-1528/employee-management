package com.example.employee_management.exception;

// Thrown by EmployeeService when the id does not exist; GlobalExceptionHandler answers 404.
public class EmployeeNotFoundException extends RuntimeException {

	public EmployeeNotFoundException(Long id) {
		super("Employee not found with id " + id);
	}

}
