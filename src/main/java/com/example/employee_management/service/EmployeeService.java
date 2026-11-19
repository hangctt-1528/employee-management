package com.example.employee_management.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
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

// @PreAuthorize: second check after the URL rules, only ADMIN can change employees.
@Service
public class EmployeeService {

	// SLF4J API; Spring Boot uses Logback behind it. The level per profile is set in application-dev.yml / -prod.yml.
	// Logs contain only ids, never names or emails (personal data).
	private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

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
	@PreAuthorize("hasRole('ADMIN')")
	public EmployeeResponse addEmployee(EmployeeRequest request) {
		logger.debug("Adding employee: departmentId={}", request.getDepartmentId());
		Department department = findDepartment(request.getDepartmentId());
		String email = normalizeEmail(request.getEmail());
		if (employeeRepository.existsByEmail(email)) {
			logger.warn("Cannot add employee: email is already used");
			throw new DuplicateEmailException(email);
		}
		Employee employee = new Employee(utilityService.formatName(request.getName()), email, department);
		// INSERT; the database gives the id
		Employee saved = employeeRepository.save(employee);
		logger.info("Employee added: id={}, departmentId={}", saved.getId(), department.getId());
		return toResponse(saved);
	}

	// Throws DepartmentNotFoundException (-> 400), EmployeeNotFoundException (-> 404)
	// or DuplicateEmailException (-> 409), checked in that order
	@PreAuthorize("hasRole('ADMIN')")
	public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
		logger.debug("Updating employee: id={}, departmentId={}", id, request.getDepartmentId());
		Department department = findDepartment(request.getDepartmentId());
		Employee employee = findEmployee(id);
		String email = normalizeEmail(request.getEmail());
		if (employeeRepository.existsByEmailAndIdNot(email, id)) {
			logger.warn("Cannot update employee id={}: email is already used by another employee", id);
			throw new DuplicateEmailException(email);
		}
		employee.setName(utilityService.formatName(request.getName()));
		employee.setEmail(email);
		employee.setDepartment(department);
		Employee saved = employeeRepository.save(employee);
		logger.info("Employee updated: id={}, departmentId={}", saved.getId(), department.getId());
		return toResponse(saved);
	}

	// Throws EmployeeNotFoundException (-> 404) when the id does not exist
	@PreAuthorize("hasRole('ADMIN')")
	public void deleteEmployee(Long id) {
		logger.debug("Deleting employee: id={}", id);
		if (!employeeRepository.existsById(id)) {
			logger.warn("Cannot delete employee id={}: not found", id);
			throw new EmployeeNotFoundException(id);
		}
		employeeRepository.deleteById(id);
		logger.info("Employee deleted: id={}", id);
	}

	private Employee findEmployee(Long id) {
		Optional<Employee> found = employeeRepository.findById(id);
		if (found.isEmpty()) {
			logger.warn("Employee not found: id={}", id);
			throw new EmployeeNotFoundException(id);
		}
		return found.get();
	}

	private Department findDepartment(Long departmentId) {
		Optional<Department> found = departmentRepository.findById(departmentId);
		if (found.isEmpty()) {
			logger.warn("Department not found: id={}", departmentId);
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
