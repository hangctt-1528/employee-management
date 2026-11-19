package com.example.employee_management.exception;

// Username already taken (shown on the register page)
public class DuplicateUsernameException extends RuntimeException {

	public DuplicateUsernameException(String username) {
		super("Username " + username + " is already taken");
	}

}
