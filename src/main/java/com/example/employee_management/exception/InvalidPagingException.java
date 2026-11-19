package com.example.employee_management.exception;

// page < 0 or size outside 1..100 -> 400
public class InvalidPagingException extends RuntimeException {

	public InvalidPagingException(String message) {
		super(message);
	}

}
