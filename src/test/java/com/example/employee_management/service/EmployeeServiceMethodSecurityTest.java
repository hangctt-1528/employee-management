package com.example.employee_management.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.exception.EmployeeNotFoundException;

// Proves the second line of defence: @PreAuthorize on EmployeeService (MethodSecurityConfig).
// The service is called directly, so no URL rule of SecurityConfig is involved.
// No data is changed: the ADMIN calls use an unknown id and stop in the service with 404-type exceptions.
@SpringBootTest
class EmployeeServiceMethodSecurityTest {

	@Autowired
	private EmployeeService employeeService;

	private EmployeeRequest request() {
		return new EmployeeRequest("an", "an@example.com", 1L);
	}

	@Test
	@WithMockUser(roles = "USER")
	void userCannotAddUpdateOrDelete() {
		assertThrows(AccessDeniedException.class, () -> employeeService.addEmployee(request()));
		assertThrows(AccessDeniedException.class, () -> employeeService.updateEmployee(999L, request()));
		assertThrows(AccessDeniedException.class, () -> employeeService.deleteEmployee(999L));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminPassesTheCheck() {
		// AccessDeniedException would mean the check failed; "not found" means the method body ran
		assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(999L));
		assertThrows(EmployeeNotFoundException.class, () -> employeeService.updateEmployee(999L, request()));
	}

	@Test
	@WithMockUser(roles = "USER")
	void userCanStillRead() {
		employeeService.getAllEmployees(); // no exception: read methods have no @PreAuthorize
	}

}
