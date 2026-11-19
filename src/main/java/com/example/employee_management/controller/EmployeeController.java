package com.example.employee_management.controller;

import java.net.URI;
import java.util.List;

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

import jakarta.validation.Valid;

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

	// Errors (400 / 404 / 409 ...) are not handled here: the service throws an exception
	// and GlobalExceptionHandler turns it into an ErrorResponse.

	// GET /api/employees/1 -> 200; 404 no employee
	@GetMapping("/{id}")
	public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long id) {
		return ResponseEntity.ok(employeeService.getEmployeeById(id));
	}

	// POST /api/employees -> 201 + Location; 400 invalid body / unknown department; 409 email used
	// @Valid checks the constraints in EmployeeRequest before the method runs
	@PostMapping
	public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
		EmployeeResponse created = employeeService.addEmployee(request);
		URI location = URI.create("/api/employees/" + created.getId());
		return ResponseEntity.created(location).body(created);
	}

	// PUT /api/employees/1 -> 200; 400 invalid body / unknown department; 404 no employee; 409 email used by another
	@PutMapping("/{id}")
	public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id,
			@Valid @RequestBody EmployeeRequest request) {
		return ResponseEntity.ok(employeeService.updateEmployee(id, request));
	}

	// DELETE /api/employees/1 -> 204; 404 no employee
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
		employeeService.deleteEmployee(id);
		return ResponseEntity.noContent().build();
	}

}
