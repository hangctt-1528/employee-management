package com.example.employee_management.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.employee_management.dto.DepartmentEmployeeCount;
import com.example.employee_management.entity.Department;
import com.example.employee_management.entity.Employee;

// The @Query countEmployeesByDepartment() on H2
@DataJpaTest
class DepartmentRepositoryTest {

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Test
	void noDepartmentsGivesAnEmptyList() {
		assertTrue(departmentRepository.countEmployeesByDepartment().isEmpty());
	}

	@Test
	void countsEmployeesPerDepartmentIncludingEmptyOnes() {
		Department it = departmentRepository.save(new Department("IT"));
		Department hr = departmentRepository.save(new Department("HR"));
		Department sales = departmentRepository.save(new Department("Sales"));
		employeeRepository.save(new Employee("An", "an@example.com", it));
		employeeRepository.save(new Employee("Binh", "binh@example.com", it));
		employeeRepository.save(new Employee("Chi", "chi@example.com", sales));

		List<DepartmentEmployeeCount> rows = departmentRepository.countEmployeesByDepartment();

		assertEquals(3, rows.size());
		// most employees first
		assertEquals("IT", rows.get(0).getDepartmentName());
		assertEquals(it.getId(), rows.get(0).getDepartmentId());
		assertEquals(2L, rows.get(0).getEmployeeCount());
		assertEquals("Sales", rows.get(1).getDepartmentName());
		assertEquals(1L, rows.get(1).getEmployeeCount());
		// LEFT JOIN: a department without employees is still there, with 0
		assertEquals("HR", rows.get(2).getDepartmentName());
		assertEquals(hr.getId(), rows.get(2).getDepartmentId());
		assertEquals(0L, rows.get(2).getEmployeeCount());
	}

	@Test
	void sameCountIsSortedByName() {
		departmentRepository.save(new Department("Sales"));
		departmentRepository.save(new Department("HR"));
		departmentRepository.save(new Department("IT"));

		List<DepartmentEmployeeCount> rows = departmentRepository.countEmployeesByDepartment();

		assertEquals("HR", rows.get(0).getDepartmentName());
		assertEquals("IT", rows.get(1).getDepartmentName());
		assertEquals("Sales", rows.get(2).getDepartmentName());
	}

}
