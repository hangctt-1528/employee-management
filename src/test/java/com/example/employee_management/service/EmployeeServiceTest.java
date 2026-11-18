package com.example.employee_management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.repository.EmployeeRepository;

// Plain JUnit: constructor injection lets us build the service by hand, no Spring needed.
class EmployeeServiceTest {

	private EmployeeService employeeService;

	@BeforeEach
	void setUp() {
		employeeService = new EmployeeService(new EmployeeRepository(), new UtilityService());
	}

	@Test
	void listIsEmptyAtStart() {
		assertTrue(employeeService.getAllEmployees().isEmpty());
	}

	@Test
	void addEmployeeFormatsDataAndGeneratesIdAndCode() {
		EmployeeResponse created = employeeService.addEmployee(
				new EmployeeRequest("  nguyEN van  AN ", " an@example.com "));

		assertEquals(1L, created.getId());
		assertEquals("EMP0001", created.getCode());
		assertEquals("Nguyen Van An", created.getName());
		assertEquals("an@example.com", created.getEmail());
	}

	@Test
	void secondEmployeeGetsNextIdAndCode() {
		employeeService.addEmployee(new EmployeeRequest("an", "an@example.com"));
		EmployeeResponse second = employeeService.addEmployee(new EmployeeRequest("binh", "binh@example.com"));

		assertEquals(2L, second.getId());
		assertEquals("EMP0002", second.getCode());
	}

	@Test
	void getAllEmployeesKeepsInsertOrder() {
		employeeService.addEmployee(new EmployeeRequest("an", "an@example.com"));
		employeeService.addEmployee(new EmployeeRequest("binh", "binh@example.com"));

		List<EmployeeResponse> employees = employeeService.getAllEmployees();
		assertEquals(2, employees.size());
		assertEquals("An", employees.get(0).getName());
		assertEquals("Binh", employees.get(1).getName());
	}

	@Test
	void getEmployeeByIdFindsEmployee() {
		employeeService.addEmployee(new EmployeeRequest("an", "an@example.com"));

		assertEquals("An", employeeService.getEmployeeById(1L).getName());
	}

	@Test
	void getEmployeeByIdReturnsNullWhenMissing() {
		assertNull(employeeService.getEmployeeById(99L));
	}

	@Test
	void searchByNameIgnoresCase() {
		employeeService.addEmployee(new EmployeeRequest("nguyen van an", "an@example.com"));
		employeeService.addEmployee(new EmployeeRequest("tran binh", "binh@example.com"));

		List<EmployeeResponse> found = employeeService.searchByName("VAN");
		assertEquals(1, found.size());
		assertEquals("Nguyen Van An", found.get(0).getName());
	}

	@Test
	void searchByNameWithNoMatchReturnsEmptyList() {
		employeeService.addEmployee(new EmployeeRequest("an", "an@example.com"));

		assertTrue(employeeService.searchByName("xyz").isEmpty());
	}

	@Test
	void searchByBlankNameReturnsEveryone() {
		employeeService.addEmployee(new EmployeeRequest("an", "an@example.com"));
		employeeService.addEmployee(new EmployeeRequest("binh", "binh@example.com"));

		assertEquals(2, employeeService.searchByName("  ").size());
	}

}
