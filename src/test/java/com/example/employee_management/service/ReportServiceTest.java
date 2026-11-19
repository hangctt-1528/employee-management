package com.example.employee_management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.employee_management.dto.DepartmentEmployeeCount;
import com.example.employee_management.dto.EmployeeCountResponse;
import com.example.employee_management.dto.EmployeeStatisticsResponse;
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

	// ---- Lab 10: statistics ----

	@Test
	void statisticsOfAnEmptyDatabase() {
		EmployeeStatisticsResponse statistics = reportService.getStatistics();

		assertEquals(0, statistics.getTotalEmployees());
		assertTrue(statistics.getDepartments().isEmpty());
		assertNotNull(statistics.getGeneratedAt());
	}

	@Test
	void statisticsTotalEqualsTheSumOfTheRows() {
		Department it = departmentRepository.save(new Department("IT"));
		Department hr = departmentRepository.save(new Department("HR"));
		departmentRepository.save(new Department("Sales"));
		employeeRepository.save(new Employee("An", "an@example.com", it));
		employeeRepository.save(new Employee("Binh", "binh@example.com", it));
		employeeRepository.save(new Employee("Chi", "chi@example.com", hr));

		EmployeeStatisticsResponse statistics = reportService.getStatistics();

		assertEquals(3, statistics.getTotalEmployees());
		assertEquals(3, statistics.getDepartments().size());
		long sum = 0;
		for (DepartmentEmployeeCount row : statistics.getDepartments()) {
			sum += row.getEmployeeCount();
		}
		assertEquals(statistics.getTotalEmployees(), sum);
	}

	@Test
	void employeesByDepartmentReturnsTheRows() {
		Department it = departmentRepository.save(new Department("IT"));
		employeeRepository.save(new Employee("An", "an@example.com", it));

		List<DepartmentEmployeeCount> rows = reportService.getEmployeesByDepartment();

		assertEquals(1, rows.size());
		assertEquals("IT", rows.get(0).getDepartmentName());
		assertEquals(1L, rows.get(0).getEmployeeCount());
	}

}
