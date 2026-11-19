package com.example.employee_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

// Table "employee" (Boot's default name for the class)
@Entity
public class Employee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	// Many employees -> one department: column department_id (NOT NULL because optional = false).
	// @ManyToOne is EAGER by default, so the department is loaded with the employee.
	@ManyToOne(optional = false)
	private Department department;

	// JPA needs an empty constructor to build objects from table rows
	public Employee() {
	}

	public Employee(String name, String email, Department department) {
		this.name = name;
		this.email = email;
		this.department = department;
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Department getDepartment() {
		return department;
	}

	public void setDepartment(Department department) {
		this.department = department;
	}

}
