package com.example.employee_management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.employee_management.dto.EmployeeCountResponse;
import com.example.employee_management.entity.Department;
import com.example.employee_management.entity.Employee;
import com.example.employee_management.repository.DepartmentRepository;
import com.example.employee_management.repository.EmployeeRepository;

// @DataJpaTest does not turn caching on, so every call counts in the database:
// this test checks the counting only. The cache (and clearing it) is tested in EmployeeCountCacheTest.
@DataJpaTest
@Import(ReportService.class)
class ReportServiceTest {

	@Autowired
	private ReportService reportService;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Test
	void emptyDatabaseCountsZero() {
		EmployeeCountResponse report = reportService.getEmployeeCount();

		assertEquals(0, report.getTotalEmployees());
		assertNotNull(report.getCountedAt());
	}

	@Test
	void countsSavedEmployees() {
		Department it = departmentRepository.save(new Department("IT"));
		employeeRepository.save(new Employee("An", "an@example.com", it));
		employeeRepository.save(new Employee("Binh", "binh@example.com", it));

		assertEquals(2, reportService.getEmployeeCount().getTotalEmployees());
	}

}
