package com.example.employee_management.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.employee_management.dto.DepartmentResponse;
import com.example.employee_management.entity.Department;
import com.example.employee_management.repository.DepartmentRepository;

@Service
public class DepartmentService {

	private final DepartmentRepository departmentRepository;

	public DepartmentService(DepartmentRepository departmentRepository) {
		this.departmentRepository = departmentRepository;
	}

	public List<DepartmentResponse> getAllDepartments() {
		List<DepartmentResponse> result = new ArrayList<>();
		for (Department department : departmentRepository.findAll()) {
			result.add(new DepartmentResponse(department.getId(), department.getName()));
		}
		return result;
	}

}
