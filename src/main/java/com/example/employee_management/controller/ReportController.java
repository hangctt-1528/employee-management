package com.example.employee_management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.employee_management.dto.EmployeeCountResponse;
import com.example.employee_management.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

	private final ReportService reportService;

	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	// GET /api/reports/employee-count -> 200; cached up to 1 minute (see ReportService)
	@GetMapping("/employee-count")
	public ResponseEntity<EmployeeCountResponse> getEmployeeCount() {
		return ResponseEntity.ok(reportService.getEmployeeCount());
	}

}
