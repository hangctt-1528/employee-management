package com.example.employee_management.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

	// GET /api/employees           -> all employees
	// GET /api/employees?name=an   -> employees whose name contains "an"
	@GetMapping
	public ResponseEntity<List<EmployeeResponse>> getEmployees(@RequestParam(required = false) String name) {
		List<EmployeeResponse> employees;
		if (isBlank(name)) {
			employees = employeeService.getAllEmployees();
		} else {
			employees = employeeService.searchByName(name);
		}
		return ResponseEntity.ok(employees);
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

	// POST /api/employees with a JSON body -> 201 + Location header, or 400 if name/email is blank
	@PostMapping
	public ResponseEntity<EmployeeResponse> createEmployee(@RequestBody EmployeeRequest request) {
		if (isBlank(request.getName()) || isBlank(request.getEmail())) {
			return ResponseEntity.badRequest().build(); // Lab 5 replaces this check with @Valid
		}
		EmployeeResponse created = employeeService.addEmployee(request);
		URI location = URI.create("/api/employees/" + created.getId());
		return ResponseEntity.created(location).body(created);
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

}
