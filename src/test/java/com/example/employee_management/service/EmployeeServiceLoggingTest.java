package com.example.employee_management.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.entity.Department;
import com.example.employee_management.exception.DepartmentNotFoundException;
import com.example.employee_management.exception.DuplicateEmailException;
import com.example.employee_management.exception.EmployeeNotFoundException;
import com.example.employee_management.repository.DepartmentRepository;

// Lab 7: checks the log lines written by EmployeeService when employees are added, updated or deleted.
// OutputCaptureExtension catches what Logback prints to the console; each test gets it as CapturedOutput.
@DataJpaTest
@Import({ EmployeeService.class, UtilityService.class })
@ExtendWith(OutputCaptureExtension.class)
class EmployeeServiceLoggingTest {

	@Autowired
	private EmployeeService employeeService;

	@Autowired
	private DepartmentRepository departmentRepository;

	private Long itId;

	@BeforeEach
	void setUp() {
		itId = departmentRepository.save(new Department("IT")).getId();
	}

	private EmployeeResponse addAn() {
		return employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", itId));
	}

	@Test
	void addLogsInfoWithIdsOnly(CapturedOutput output) {
		EmployeeResponse created = addAn();

		String log = output.getOut();
		assertTrue(log.contains("INFO"));
		assertTrue(log.contains("Employee added: id=" + created.getId() + ", departmentId=" + itId));
		assertFalse(log.contains("an@example.com")); // no personal data in the log
	}

	@Test
	void duplicateEmailOnAddLogsWarn(CapturedOutput output) {
		addAn();

		assertThrows(DuplicateEmailException.class, () -> addAn());

		String log = output.getOut();
		assertTrue(log.contains("WARN"));
		assertTrue(log.contains("Cannot add employee: email is already used"));
		assertFalse(log.contains("an@example.com"));
	}

	@Test
	void unknownDepartmentLogsWarn(CapturedOutput output) {
		assertThrows(DepartmentNotFoundException.class,
				() -> employeeService.addEmployee(new EmployeeRequest("an", "an@example.com", 999L)));

		assertTrue(output.getOut().contains("Department not found: id=999"));
	}

	@Test
	void updateLogsInfo(CapturedOutput output) {
		Long id = addAn().getId();

		employeeService.updateEmployee(id, new EmployeeRequest("an", "an2@example.com", itId));

		assertTrue(output.getOut().contains("Employee updated: id=" + id + ", departmentId=" + itId));
	}

	@Test
	void updateUnknownEmployeeLogsWarn(CapturedOutput output) {
		assertThrows(EmployeeNotFoundException.class,
				() -> employeeService.updateEmployee(999L, new EmployeeRequest("an", "an@example.com", itId)));

		String log = output.getOut();
		assertTrue(log.contains("Employee not found: id=999"));
		assertFalse(log.contains("Employee updated"));
	}

	@Test
	void deleteLogsInfo(CapturedOutput output) {
		Long id = addAn().getId();

		employeeService.deleteEmployee(id);

		assertTrue(output.getOut().contains("Employee deleted: id=" + id));
	}

	@Test
	void deleteUnknownEmployeeLogsWarn(CapturedOutput output) {
		assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(999L));

		String log = output.getOut();
		assertTrue(log.contains("Cannot delete employee id=999: not found"));
		assertFalse(log.contains("Employee deleted"));
	}

	// Tests run without a profile, so the level is the default INFO and DEBUG lines are filtered out
	@Test
	void debugLinesAreHiddenWithoutDevProfile(CapturedOutput output) {
		addAn();

		assertFalse(output.getOut().contains("Adding employee"));
	}

}
