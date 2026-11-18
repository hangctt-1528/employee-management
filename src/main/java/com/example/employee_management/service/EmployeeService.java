package com.example.employee_management.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.entity.Department;
import com.example.employee_management.entity.Employee;
import com.example.employee_management.exception.DepartmentNotFoundException;
import com.example.employee_management.exception.DuplicateEmailException;
import com.example.employee_management.exception.EmployeeNotFoundException;
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

	// Throws EmployeeNotFoundException (-> 404) when the id does not exist
	public EmployeeResponse getEmployeeById(Long id) {
		return toResponse(findEmployee(id));
	}

	// Throws DepartmentNotFoundException (-> 400) or DuplicateEmailException (-> 409)
	public EmployeeResponse addEmployee(EmployeeRequest request) {
		Department department = findDepartment(request.getDepartmentId());
		String email = normalizeEmail(request.getEmail());
		if (employeeRepository.existsByEmail(email)) {
			throw new DuplicateEmailException(email);
		}
		Employee employee = new Employee(utilityService.formatName(request.getName()), email, department);
		// INSERT; the database gives the id
		return toResponse(employeeRepository.save(employee));
	}

	// Throws DepartmentNotFoundException (-> 400), EmployeeNotFoundException (-> 404)
	// or DuplicateEmailException (-> 409), checked in that order
	public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
		Department department = findDepartment(request.getDepartmentId());
		Employee employee = findEmployee(id);
		String email = normalizeEmail(request.getEmail());
		if (employeeRepository.existsByEmailAndIdNot(email, id)) {
			throw new DuplicateEmailException(email);
		}
		employee.setName(utilityService.formatName(request.getName()));
		employee.setEmail(email);
		employee.setDepartment(department);
		return toResponse(employeeRepository.save(employee));
	}

	// Throws EmployeeNotFoundException (-> 404) when the id does not exist
	public void deleteEmployee(Long id) {
		if (!employeeRepository.existsById(id)) {
			throw new EmployeeNotFoundException(id);
		}
		employeeRepository.deleteById(id);
	}

	private Employee findEmployee(Long id) {
		Optional<Employee> found = employeeRepository.findById(id);
		if (found.isEmpty()) {
			throw new EmployeeNotFoundException(id);
		}
		return found.get();
	}

	private Department findDepartment(Long departmentId) {
		Optional<Department> found = departmentRepository.findById(departmentId);
		if (found.isEmpty()) {
			throw new DepartmentNotFoundException(departmentId);
		}
		return found.get();
	}

	// Emails are stored in lower case, so "An@Example.com" and "an@example.com" are the same
	// on both MySQL (case-insensitive) and H2 (case-sensitive)
	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
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
