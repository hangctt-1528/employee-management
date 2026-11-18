package com.example.employee_management.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.employee_management.entity.Department;
import com.example.employee_management.repository.DepartmentRepository;

@Component
public class DataSeeder implements CommandLineRunner {

	private final DepartmentRepository departmentRepository;

	public DataSeeder(DepartmentRepository departmentRepository) {
		this.departmentRepository = departmentRepository;
	}

	@Override
	public void run(String... args) {
		if (departmentRepository.count() > 0) {
			return;
		}
		departmentRepository.save(new Department("IT"));
		departmentRepository.save(new Department("HR"));
		departmentRepository.save(new Department("Sales"));
	}

}
