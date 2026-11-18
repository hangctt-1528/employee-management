package com.example.employee_management.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.service.EmployeeService;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	// GET /api/employees                        -> all employees
	// GET /api/employees?name=an                -> name contains "an"
	// GET /api/employees?departmentId=1         -> employees of department 1
	// GET /api/employees?name=an&departmentId=1 -> both conditions (AND)
	@GetMapping
	public ResponseEntity<List<EmployeeResponse>> getEmployees(@RequestParam(required = false) String name,
			@RequestParam(required = false) Long departmentId) {
		return ResponseEntity.ok(employeeService.searchEmployees(name, departmentId));
	}

	// GET /api/employees/1 -> 200 with the employee, or 404
	@GetMapping("/{id}")
	public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long id) {
		EmployeeResponse employee = employeeService.getEmployeeById(id);
		if (employee == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(employee);
	}

	// POST /api/employees -> 201 + Location; 400 bad body or unknown department; 409 email already used
	@PostMapping
	public ResponseEntity<EmployeeResponse> createEmployee(@RequestBody EmployeeRequest request) {
		if (!isValid(request) || !employeeService.departmentExists(request.getDepartmentId())) {
			return ResponseEntity.badRequest().build(); // Lab 5 replaces these checks with @Valid
		}
		if (employeeService.emailExists(request.getEmail())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		EmployeeResponse created = employeeService.addEmployee(request);
		URI location = URI.create("/api/employees/" + created.getId());
		return ResponseEntity.created(location).body(created);
	}

	// PUT /api/employees/1 -> 200; 400 bad body or unknown department; 404 no employee; 409 email used by another
	@PutMapping("/{id}")
	public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id,
			@RequestBody EmployeeRequest request) {
		if (!isValid(request) || !employeeService.departmentExists(request.getDepartmentId())) {
			return ResponseEntity.badRequest().build();
		}
		if (!employeeService.employeeExists(id)) {
			return ResponseEntity.notFound().build();
		}
		if (employeeService.emailUsedByOther(request.getEmail(), id)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		return ResponseEntity.ok(employeeService.updateEmployee(id, request));
	}

	// DELETE /api/employees/1 -> 204, or 404
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
		if (!employeeService.deleteEmployee(id)) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.noContent().build();
	}

	// name and email not blank, departmentId given
	private boolean isValid(EmployeeRequest request) {
		return !isBlank(request.getName()) && !isBlank(request.getEmail()) && request.getDepartmentId() != null;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

}
