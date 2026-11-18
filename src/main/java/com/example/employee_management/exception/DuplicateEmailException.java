package com.example.employee_management.exception;

// Thrown by EmployeeService when another employee already has the email; GlobalExceptionHandler answers 409.
public class DuplicateEmailException extends RuntimeException {

	public DuplicateEmailException(String email) {
		super("Email " + email + " is already used by another employee");
	}

}
