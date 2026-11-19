package com.example.employee_management.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.employee_management.dto.DepartmentResponse;
import com.example.employee_management.service.DepartmentService;

// Read-only: lets clients see which departmentId to send when adding an employee
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

	private final DepartmentService departmentService;

	public DepartmentController(DepartmentService departmentService) {
		this.departmentService = departmentService;
	}

	@GetMapping
	public ResponseEntity<List<DepartmentResponse>> getDepartments() {
		return ResponseEntity.ok(departmentService.getAllDepartments());
	}

}
