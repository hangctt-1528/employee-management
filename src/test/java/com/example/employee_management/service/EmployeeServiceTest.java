package com.example.employee_management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.employee_management.dto.EmployeePageResponse;
import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.entity.Department;
import com.example.employee_management.exception.DepartmentNotFoundException;
import com.example.employee_management.exception.DuplicateEmailException;
import com.example.employee_management.exception.EmployeeNotFoundException;
import com.example.employee_management.exception.InvalidPagingException;
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

	private EmployeeResponse addAn() {
		return employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", itId));
	}

	@Test
	void addEmployeeFormatsDataAndSetsDepartment() {
		EmployeeResponse created = employeeService.addEmployee(
				new EmployeeRequest("  nguyEN van  AN ", "an@example.com", itId));

		assertEquals("Nguyen Van An", created.getName());
		assertEquals("an@example.com", created.getEmail());
		assertEquals(itId, created.getDepartmentId());
		assertEquals("IT", created.getDepartmentName());
		assertNotNull(created.getId());
	}

	@Test
	void emailIsStoredInLowerCase() {
		EmployeeResponse created = employeeService.addEmployee(new EmployeeRequest("an", "An@Example.COM", itId));

		assertEquals("an@example.com", created.getEmail());
	}

	@Test
	void getEmployeeByIdFindsSavedEmployee() {
		EmployeeResponse created = addAn();

		assertEquals("An", employeeService.getEmployeeById(created.getId()).getName());
	}

	@Test
	void getEmployeeByIdThrowsWhenMissing() {
		EmployeeNotFoundException ex = assertThrows(EmployeeNotFoundException.class,
				() -> employeeService.getEmployeeById(999L));
		assertEquals("Employee not found with id 999", ex.getMessage());
	}

	@Test
	void searchByNameIgnoresCase() {
		employeeService.addEmployee(new EmployeeRequest("nguyen van an", "an@example.com", itId));
		employeeService.addEmployee(new EmployeeRequest("tran binh", "binh@example.com", hrId));

		List<EmployeeResponse> found = employeeService.getEmployeePage("VAN", null, 0, 10).getContent();
		assertEquals(1, found.size());
		assertEquals("Nguyen Van An", found.get(0).getName());
	}

	@Test
	void searchByDepartment() {
		employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", itId));
		employeeService.addEmployee(new EmployeeRequest("binh", "binh@example.com", hrId));

		List<EmployeeResponse> found = employeeService.getEmployeePage(null, hrId, 0, 10).getContent();
		assertEquals(1, found.size());
		assertEquals("Binh", found.get(0).getName());
	}

	@Test
	void searchByNameAndDepartmentUsesAnd() {
		employeeService.addEmployee(new EmployeeRequest("an it", "an1@example.com", itId));
		employeeService.addEmployee(new EmployeeRequest("an hr", "an2@example.com", hrId));

		List<EmployeeResponse> found = employeeService.getEmployeePage("an", hrId, 0, 10).getContent();
		assertEquals(1, found.size());
		assertEquals("An Hr", found.get(0).getName());
	}

	@Test
	void searchWithoutConditionsReturnsEveryone() {
		employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", itId));
		employeeService.addEmployee(new EmployeeRequest("binh", "binh@example.com", hrId));

		assertEquals(2, employeeService.getEmployeePage("  ", null, 0, 10).getContent().size());
	}

	@Test
	void searchWithUnknownDepartmentReturnsEmptyList() {
		addAn();

		assertEquals(0, employeeService.getEmployeePage(null, 999L, 0, 10).getContent().size());
	}

	@Test
	void addWithUnknownDepartmentThrows() {
		assertThrows(DepartmentNotFoundException.class,
				() -> employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", 999L)));
	}

	@Test
	void addWithUsedEmailThrowsEvenWithOtherCase() {
		addAn();

		assertThrows(DuplicateEmailException.class,
				() -> employeeService.addEmployee(new EmployeeRequest("binh", "AN@example.com", hrId)));
	}

	@Test
	void updateEmployeeChangesAllFields() {
		EmployeeResponse created = addAn();

		EmployeeResponse updated = employeeService.updateEmployee(created.getId(),
				new EmployeeRequest("an moi", "new@example.com", hrId));

		assertEquals("An Moi", updated.getName());
		assertEquals("new@example.com", updated.getEmail());
		assertEquals("HR", updated.getDepartmentName());
		assertEquals(created.getId(), updated.getId()); // same employee, not a new row
	}

	@Test
	void updateKeepingOwnEmailIsAllowed() {
		EmployeeResponse created = addAn();

		EmployeeResponse updated = employeeService.updateEmployee(created.getId(),
				new EmployeeRequest("an moi", "an@example.com", itId));

		assertEquals("An Moi", updated.getName());
	}

	@Test
	void updateMissingEmployeeThrows() {
		assertThrows(EmployeeNotFoundException.class,
				() -> employeeService.updateEmployee(999L, new EmployeeRequest("an", "an@example.com", itId)));
	}

	@Test
	void updateWithUnknownDepartmentThrows() {
		EmployeeResponse created = addAn();

		assertThrows(DepartmentNotFoundException.class, () -> employeeService.updateEmployee(created.getId(),
				new EmployeeRequest("an", "an@example.com", 999L)));
	}

	@Test
	void updateWithEmailOfAnotherEmployeeThrows() {
		addAn();
		EmployeeResponse binh = employeeService.addEmployee(new EmployeeRequest("binh", "binh@example.com", hrId));

		assertThrows(DuplicateEmailException.class, () -> employeeService.updateEmployee(binh.getId(),
				new EmployeeRequest("binh", "an@example.com", hrId)));
	}

	@Test
	void deleteEmployeeRemovesIt() {
		EmployeeResponse created = addAn();

		employeeService.deleteEmployee(created.getId());

		assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(created.getId()));
	}

	@Test
	void deleteMissingEmployeeThrows() {
		assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(999L));
	}

	// ---- Pagination ----

	private void addEmployees(int count) {
		for (int i = 1; i <= count; i++) {
			employeeService.addEmployee(new EmployeeRequest("employee " + i, "e" + i + "@example.com", itId));
		}
	}

	@Test
	void pagesSplitTheEmployeesInIdOrder() {
		addEmployees(5);

		EmployeePageResponse first = employeeService.getEmployeePage(null, null, 0, 2);
		EmployeePageResponse last = employeeService.getEmployeePage(null, null, 2, 2);

		assertEquals(5, first.getTotalElements());
		assertEquals(3, first.getTotalPages());
		assertEquals(2, first.getContent().size());
		assertTrue(first.isFirst());
		assertFalse(first.isLast());
		assertEquals("Employee 1", first.getContent().get(0).getName());
		assertEquals(1, last.getContent().size()); // 5 = 2 + 2 + 1
		assertEquals("Employee 5", last.getContent().get(0).getName());
		assertTrue(last.isLast());
		assertEquals(5, last.fromItem());
		assertEquals(5, last.toItem());
	}

	@Test
	void filtersArePagedToo() {
		addEmployees(3);
		employeeService.addEmployee(new EmployeeRequest("other", "other@example.com", hrId));

		EmployeePageResponse page = employeeService.getEmployeePage("employee", itId, 0, 2);

		assertEquals(3, page.getTotalElements());
		assertEquals(2, page.getContent().size());
	}

	@Test
	void pageAfterTheLastOneIsEmpty() {
		addEmployees(3);

		EmployeePageResponse page = employeeService.getEmployeePage(null, null, 5, 10);

		assertTrue(page.getContent().isEmpty());
		assertEquals(3, page.getTotalElements());
	}

	@Test
	void pageAndSizeLimits() {
		assertThrows(InvalidPagingException.class, () -> employeeService.getEmployeePage(null, null, -1, 10));
		assertThrows(InvalidPagingException.class, () -> employeeService.getEmployeePage(null, null, 0, 0));
		assertThrows(InvalidPagingException.class, () -> employeeService.getEmployeePage(null, null, 0, 101));
		// offset page * size would not fit in an int
		assertThrows(InvalidPagingException.class, () -> employeeService.getEmployeePage(null, null, 21474837, 100));
		// limits that are allowed
		assertEquals(0, employeeService.getEmployeePage(null, null, 0, 1).getTotalElements());
		assertEquals(0, employeeService.getEmployeePage(null, null, 0, 100).getTotalElements());
	}

}
