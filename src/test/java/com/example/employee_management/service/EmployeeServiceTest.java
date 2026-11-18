package com.example.employee_management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.entity.Department;
import com.example.employee_management.repository.DepartmentRepository;

// @DataJpaTest starts only JPA (on H2) and rolls back after each test.
// @Import adds the two services, so they run against the real repositories.
@DataJpaTest
@Import({ EmployeeService.class, UtilityService.class })
class EmployeeServiceTest {

	@Autowired
	private EmployeeService employeeService;

	@Autowired
	private DepartmentRepository departmentRepository;

	private Long itId;
	private Long hrId;

	@BeforeEach
	void setUp() {
		itId = departmentRepository.save(new Department("IT")).getId();
		hrId = departmentRepository.save(new Department("HR")).getId();
	}

	@Test
	void addEmployeeFormatsDataAndSetsDepartment() {
		EmployeeResponse created = employeeService.addEmployee(
				new EmployeeRequest("  nguyEN van  AN ", " an@example.com ", itId));

		assertEquals("Nguyen Van An", created.getName());
		assertEquals("an@example.com", created.getEmail());
		assertEquals(itId, created.getDepartmentId());
		assertEquals("IT", created.getDepartmentName());
		assertNotNull(created.getId());
	}

	@Test
	void getEmployeeByIdFindsSavedEmployee() {
		EmployeeResponse created = employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", itId));

		assertEquals("An", employeeService.getEmployeeById(created.getId()).getName());
	}

	@Test
	void getEmployeeByIdReturnsNullWhenMissing() {
		assertNull(employeeService.getEmployeeById(999L));
	}

	@Test
	void searchByNameIgnoresCase() {
		employeeService.addEmployee(new EmployeeRequest("nguyen van an", "an@example.com", itId));
		employeeService.addEmployee(new EmployeeRequest("tran binh", "binh@example.com", hrId));

		List<EmployeeResponse> found = employeeService.searchEmployees("VAN", null);
		assertEquals(1, found.size());
		assertEquals("Nguyen Van An", found.get(0).getName());
	}

	@Test
	void searchByDepartment() {
		employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", itId));
		employeeService.addEmployee(new EmployeeRequest("binh", "binh@example.com", hrId));

		List<EmployeeResponse> found = employeeService.searchEmployees(null, hrId);
		assertEquals(1, found.size());
		assertEquals("Binh", found.get(0).getName());
	}

	@Test
	void searchByNameAndDepartmentUsesAnd() {
		employeeService.addEmployee(new EmployeeRequest("an it", "an1@example.com", itId));
		employeeService.addEmployee(new EmployeeRequest("an hr", "an2@example.com", hrId));

		List<EmployeeResponse> found = employeeService.searchEmployees("an", hrId);
		assertEquals(1, found.size());
		assertEquals("An Hr", found.get(0).getName());
	}

	@Test
	void searchWithoutConditionsReturnsEveryone() {
		employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", itId));
		employeeService.addEmployee(new EmployeeRequest("binh", "binh@example.com", hrId));

		assertEquals(2, employeeService.searchEmployees("  ", null).size());
	}

	@Test
	void updateEmployeeChangesAllFields() {
		EmployeeResponse created = employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", itId));

		EmployeeResponse updated = employeeService.updateEmployee(created.getId(),
				new EmployeeRequest("an moi", "new@example.com", hrId));

		assertEquals("An Moi", updated.getName());
		assertEquals("new@example.com", updated.getEmail());
		assertEquals("HR", updated.getDepartmentName());
		assertEquals(created.getId(), updated.getId()); // same employee, not a new row
	}

	@Test
	void updateMissingEmployeeReturnsNull() {
		assertNull(employeeService.updateEmployee(999L, new EmployeeRequest("an", "an@example.com", itId)));
	}

	@Test
	void deleteEmployeeRemovesIt() {
		EmployeeResponse created = employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", itId));

		assertTrue(employeeService.deleteEmployee(created.getId()));
		assertNull(employeeService.getEmployeeById(created.getId()));
	}

	@Test
	void deleteMissingEmployeeReturnsFalse() {
		assertFalse(employeeService.deleteEmployee(999L));
	}

	@Test
	void emailChecks() {
		EmployeeResponse created = employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", itId));

		assertTrue(employeeService.emailExists(" an@example.com "));
		assertFalse(employeeService.emailExists("other@example.com"));
		assertFalse(employeeService.emailUsedByOther("an@example.com", created.getId())); // its own email
		assertTrue(employeeService.emailUsedByOther("an@example.com", created.getId() + 1));
	}

	@Test
	void emailIsStoredInLowerCaseAndComparedIgnoringCase() {
		EmployeeResponse created = employeeService.addEmployee(
				new EmployeeRequest("an", " An@Example.COM ", itId));

		assertEquals("an@example.com", created.getEmail());
		assertTrue(employeeService.emailExists("AN@EXAMPLE.com"));
	}

	@Test
	void addEmployeeWithUnknownDepartmentReturnsNull() {
		assertNull(employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", 999L)));
	}

	@Test
	void updateWithUnknownDepartmentReturnsNull() {
		EmployeeResponse created = employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", itId));

		assertNull(employeeService.updateEmployee(created.getId(), new EmployeeRequest("an", "an@example.com", 999L)));
	}

	@Test
	void employeeExistsChecksTheTable() {
		EmployeeResponse created = employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", itId));

		assertTrue(employeeService.employeeExists(created.getId()));
		assertFalse(employeeService.employeeExists(999L));
	}

	@Test
	void departmentExistsChecksTheTable() {
		assertTrue(employeeService.departmentExists(itId));
		assertFalse(employeeService.departmentExists(999L));
	}

}
