package com.example.employee_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// Table "department" (Boot's default name for the class)
@Entity
public class Department {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	// JPA needs an empty constructor to build objects from table rows
	public Department() {
	}

	public Department(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
