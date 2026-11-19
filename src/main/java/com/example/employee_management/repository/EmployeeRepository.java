package com.example.employee_management.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.employee_management.entity.Employee;

// All methods are synchronized: the bean is a singleton shared by all request threads.
@Repository
public class EmployeeRepository {

	private final List<Employee> employees = new ArrayList<>();
	private long nextId = 1;

	// Returns a copy, so callers can't change the stored list directly
	public synchronized List<Employee> findAll() {
		return new ArrayList<>(employees);
	}

	// Returns null when no employee has this id
	public synchronized Employee findById(Long id) {
		for (Employee employee : employees) {
			if (employee.getId().equals(id)) { // Long is an object: use equals, not ==
				return employee;
			}
		}
		return null;
	}

	public synchronized Employee save(Employee employee) {
		employee.setId(nextId);
		nextId++;
		employees.add(employee);
		return employee;
	}

}
