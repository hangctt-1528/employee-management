package com.example.employee_management.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import com.example.employee_management.entity.Department;
import com.example.employee_management.entity.Employee;

// Checks the tables, the relation and the query methods on H2.
@DataJpaTest
class EmployeeRepositoryTest {

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	private Department it;
	private Department hr;

	@BeforeEach
	void setUp() {
		it = departmentRepository.save(new Department("IT"));
		hr = departmentRepository.save(new Department("HR"));
	}

	@Test
	void saveGivesIdAndKeepsDepartment() {
		Employee saved = employeeRepository.save(new Employee("Nguyen Van An", "an@example.com", it));

		assertNotNull(saved.getId());
		assertEquals("IT", employeeRepository.findById(saved.getId()).get().getDepartment().getName());
	}

	@Test
	void findByNameContainingIgnoreCase() {
		employeeRepository.save(new Employee("Nguyen Van An", "an@example.com", it));
		employeeRepository.save(new Employee("Tran Binh", "binh@example.com", it));

		assertEquals(1, employeeRepository.findByNameContainingIgnoreCase("van", PageRequest.of(0, 10)).getTotalElements());
	}

	@Test
	void findByDepartmentId() {
		employeeRepository.save(new Employee("An", "an@example.com", it));
		employeeRepository.save(new Employee("Binh", "binh@example.com", hr));

		assertEquals(1, employeeRepository.findByDepartmentId(hr.getId(), PageRequest.of(0, 10)).getTotalElements());
	}

	@Test
	void findByNameAndDepartment() {
		employeeRepository.save(new Employee("An It", "an1@example.com", it));
		employeeRepository.save(new Employee("An Hr", "an2@example.com", hr));

		assertEquals(1, employeeRepository.findByNameContainingIgnoreCaseAndDepartmentId("an", it.getId(), PageRequest.of(0, 10)).getTotalElements());
	}

	@Test
	void existsByEmail() {
		employeeRepository.save(new Employee("An", "an@example.com", it));

		assertTrue(employeeRepository.existsByEmail("an@example.com"));
	}

	@Test
	void duplicateEmailIsRejectedByTheDatabase() {
		employeeRepository.save(new Employee("An", "an@example.com", it));

		// saveAndFlush sends the INSERT right away, so the UNIQUE constraint is checked here
		assertThrows(DataIntegrityViolationException.class,
				() -> employeeRepository.saveAndFlush(new Employee("Binh", "an@example.com", it)));
	}

	@Test
	void findByIdWorksForIdAbove127() {
		Employee last = null;
		for (int i = 1; i <= 130; i++) {
			last = employeeRepository.save(new Employee("An", "an" + i + "@example.com", it));
		}
		assertTrue(employeeRepository.findById(last.getId()).isPresent());
	}

}
