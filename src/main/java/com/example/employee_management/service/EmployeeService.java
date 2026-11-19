package com.example.employee_management.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.entity.Department;
import com.example.employee_management.entity.Employee;
import com.example.employee_management.repository.DepartmentRepository;
import com.example.employee_management.repository.EmployeeRepository;

@Service
public class EmployeeService {

	private final EmployeeRepository employeeRepository;
	private final DepartmentRepository departmentRepository;
	private final UtilityService utilityService;

	public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository,
			UtilityService utilityService) {
		this.employeeRepository = employeeRepository;
		this.departmentRepository = departmentRepository;
		this.utilityService = utilityService;
	}

	public List<EmployeeResponse> getAllEmployees() {
		return toResponseList(employeeRepository.findAll());
	}

	// Search by name (contains, ignoring case), by department, or both (AND).
	// No condition -> everyone.
	public List<EmployeeResponse> searchEmployees(String name, Long departmentId) {
		boolean hasName = name != null && !name.trim().isEmpty();
		boolean hasDepartment = departmentId != null;

		List<Employee> employees;
		if (hasName && hasDepartment) {
			employees = employeeRepository.findByNameContainingIgnoreCaseAndDepartmentId(name.trim(), departmentId);
		} else if (hasName) {
			employees = employeeRepository.findByNameContainingIgnoreCase(name.trim());
		} else if (hasDepartment) {
			employees = employeeRepository.findByDepartmentId(departmentId);
		} else {
			employees = employeeRepository.findAll();
		}
		return toResponseList(employees);
	}

	// Returns null when not found; the controller turns that into 404
	public EmployeeResponse getEmployeeById(Long id) {
		Optional<Employee> employee = employeeRepository.findById(id);
		if (employee.isEmpty()) {
			return null;
		}
		return toResponse(employee.get());
	}

	public boolean departmentExists(Long departmentId) {
		return departmentRepository.existsById(departmentId);
	}

	public boolean employeeExists(Long id) {
		return employeeRepository.existsById(id);
	}

	public boolean emailExists(String email) {
		return employeeRepository.existsByEmail(normalizeEmail(email));
	}

	// For update: the email belongs to an employee other than this one
	public boolean emailUsedByOther(String email, Long id) {
		return employeeRepository.existsByEmailAndIdNot(normalizeEmail(email), id);
	}

	// Returns null when the department does not exist (the controller normally checks it first)
	public EmployeeResponse addEmployee(EmployeeRequest request) {
		Optional<Department> department = departmentRepository.findById(request.getDepartmentId());
		if (department.isEmpty()) {
			return null;
		}
		Employee employee = new Employee(utilityService.formatName(request.getName()),
				normalizeEmail(request.getEmail()), department.get());

		// INSERT; the database gives the id
		return toResponse(employeeRepository.save(employee));
	}

	// Returns null when the employee or the department does not exist
	public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
		Optional<Employee> found = employeeRepository.findById(id);
		Optional<Department> department = departmentRepository.findById(request.getDepartmentId());
		if (found.isEmpty() || department.isEmpty()) {
			return null;
		}
		Employee employee = found.get();
		employee.setName(utilityService.formatName(request.getName()));
		employee.setEmail(normalizeEmail(request.getEmail()));
		employee.setDepartment(department.get());
		return toResponse(employeeRepository.save(employee));
	}

	// Emails are stored in lower case, so "An@Example.com" and "an@example.com" are the same
	// on both MySQL (case-insensitive) and H2 (case-sensitive)
	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}

	// Returns false when the employee does not exist
	public boolean deleteEmployee(Long id) {
		if (!employeeRepository.existsById(id)) {
			return false;
		}
		employeeRepository.deleteById(id);
		return true;
	}

	private List<EmployeeResponse> toResponseList(List<Employee> employees) {
		List<EmployeeResponse> result = new ArrayList<>();
		for (Employee employee : employees) {
			result.add(toResponse(employee));
		}
		return result;
	}

	private EmployeeResponse toResponse(Employee employee) {
		Department department = employee.getDepartment();
		return new EmployeeResponse(employee.getId(), employee.getName(), employee.getEmail(), department.getId(),
				department.getName());
	}

}
