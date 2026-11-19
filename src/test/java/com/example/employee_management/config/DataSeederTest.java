package com.example.employee_management.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.employee_management.repository.DepartmentRepository;

@SpringBootTest
class DataSeederTest {

	@Autowired
	private DataSeeder dataSeeder;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Test
	void seedsThreeDepartmentsAtStartup() {
		assertEquals(3, departmentRepository.count());
	}

	@Test
	void runningAgainDoesNotAddDuplicates() {
		dataSeeder.run();
		assertEquals(3, departmentRepository.count());
	}

}
