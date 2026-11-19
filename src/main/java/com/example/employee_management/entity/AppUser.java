package com.example.employee_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// User entity. Named AppUser: "user" is reserved in H2/Postgres and Spring Security has a User class.
@Entity
@Table(name = "app_user")
public class AppUser {

	// Stored without "ROLE_"; Spring adds it
	public static final String ADMIN = "ADMIN";
	public static final String USER = "USER";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Lower case
	@Column(name = "username", nullable = false, unique = true, length = 50)
	private String username;

	// BCrypt hash
	@Column(name = "password", nullable = false, length = 100)
	private String password;

	@Column(name = "role", nullable = false, length = 20)
	private String role;

	public AppUser() {
	}

	public AppUser(String username, String password, String role) {
		this.username = username;
		this.password = password;
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

}
