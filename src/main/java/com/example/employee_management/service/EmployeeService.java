package com.example.employee_management.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.entity.Employee;
import com.example.employee_management.repository.EmployeeRepository;

@Service
public class EmployeeService {

	private final EmployeeRepository employeeRepository;
	private final UtilityService utilityService;

	public EmployeeService(EmployeeRepository employeeRepository, UtilityService utilityService) {
		this.employeeRepository = employeeRepository;
		this.utilityService = utilityService;
	}

	public List<EmployeeResponse> getAllEmployees() {
		List<EmployeeResponse> result = new ArrayList<>();
		for (Employee employee : employeeRepository.findAll()) {
			result.add(toResponse(employee));
		}
		return result;
	}

	// Name contains the keyword, ignoring case. A blank keyword returns everyone.
	public List<EmployeeResponse> searchByName(String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return getAllEmployees();
		}
		String lowerKeyword = keyword.trim().toLowerCase();
		List<EmployeeResponse> result = new ArrayList<>();
		for (Employee employee : employeeRepository.findAll()) {
			if (employee.getName().toLowerCase().contains(lowerKeyword)) {
				result.add(toResponse(employee));
			}
		}
		return result;
	}

	// Returns null when not found; the controller turns that into 404
	public EmployeeResponse getEmployeeById(Long id) {
		Employee employee = employeeRepository.findById(id);
		if (employee == null) {
			return null;
		}
		return toResponse(employee);
	}

	// The controller has already checked that name and email are not blank
	public EmployeeResponse addEmployee(EmployeeRequest request) {
		String name = utilityService.formatName(request.getName());
		String email = request.getEmail().trim();
		String code = utilityService.generateEmployeeCode();
		Employee saved = employeeRepository.save(new Employee(code, name, email));
		return toResponse(saved);
	}

	private EmployeeResponse toResponse(Employee employee) {
		return new EmployeeResponse(employee.getId(), employee.getCode(), employee.getName(), employee.getEmail());
	}

}
