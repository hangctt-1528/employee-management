package com.example.employee_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Form of /register. No role field: new accounts are always USER.
public class RegisterForm {

	public static final int MIN_PASSWORD_LENGTH = 8; // also used by UserSeeder
	public static final int MAX_PASSWORD_BYTES = 72; // limit of BCrypt, checked in AuthWebController

	// No ':' (it would break HTTP Basic "username:password")
	@NotBlank(message = "Username must not be blank")
	@Size(min = 3, max = 50, message = "Username must be 3 to 50 characters")
	@Pattern(regexp = "[A-Za-z0-9._-]*", message = "Username may contain only letters, digits, '.', '_' and '-'")
	private String username;

	@NotBlank(message = "Password must not be blank")
	@Size(min = MIN_PASSWORD_LENGTH, message = "Password must have at least 8 characters")
	private String password;

	private String confirmPassword;

	public RegisterForm() {
	}

	public RegisterForm(String username, String password, String confirmPassword) {
		setUsername(username);
		this.password = password;
		this.confirmPassword = confirmPassword;
	}

	public String getUsername() {
		return username;
	}

	// Trimmed before @Valid (the password is not)
	public void setUsername(String username) {
		if (username == null) {
			this.username = null;
		} else {
			this.username = username.trim();
		}
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

}
