package com.example.employee_management.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.employee_management.dto.EmployeePageResponse;
import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.entity.Department;
import com.example.employee_management.entity.Employee;
import com.example.employee_management.exception.DepartmentNotFoundException;
import com.example.employee_management.exception.DuplicateEmailException;
import com.example.employee_management.exception.EmployeeNotFoundException;
import com.example.employee_management.exception.InvalidPagingException;
import com.example.employee_management.repository.DepartmentRepository;
import com.example.employee_management.repository.EmployeeRepository;

// @PreAuthorize: second check after the URL rules, only ADMIN can change employees.
// @Transactional: reads are read-only; each write runs its checks and its change in one transaction.
@Service
@Transactional(readOnly = true)
public class EmployeeService {

	public static final int MAX_PAGE_SIZE = 100;

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

	// One page of employees, filtered by name (contains, ignoring case), by department, or both (AND).
	// No condition -> everyone. Always sorted by id, so the pages do not overlap.
	// page is 0-based; a page after the last one is simply empty.
	public EmployeePageResponse getEmployeePage(String name, Long departmentId, int page, int size) {
		if (page < 0) {
			throw new InvalidPagingException("page must be 0 or more");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new InvalidPagingException("size must be between 1 and " + MAX_PAGE_SIZE);
		}
		// The offset (page * size) must fit in an int, otherwise Spring Data fails with a 500
		if ((long) page * size >= Integer.MAX_VALUE) {
			throw new InvalidPagingException("page is too large");
		}
		Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
		boolean hasName = name != null && !name.trim().isEmpty();
		boolean hasDepartment = departmentId != null;

		Page<Employee> result;
		if (hasName && hasDepartment) {
			result = employeeRepository.findByNameContainingIgnoreCaseAndDepartmentId(name.trim(), departmentId,
					pageable);
		} else if (hasName) {
			result = employeeRepository.findByNameContainingIgnoreCase(name.trim(), pageable);
		} else if (hasDepartment) {
			result = employeeRepository.findByDepartmentId(departmentId, pageable);
		} else {
			result = employeeRepository.findAll(pageable);
		}
		return new EmployeePageResponse(toResponseList(result.getContent()), result.getNumber(), result.getSize(),
				result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
	}

	// Throws EmployeeNotFoundException (-> 404) when the id does not exist
	public EmployeeResponse getEmployeeById(Long id) {
		return toResponse(findEmployee(id));
	}

	// Throws DepartmentNotFoundException (-> 400) or DuplicateEmailException (-> 409)
	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
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
	@Transactional
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
		// No save(): the entity is managed. flush() sends the UPDATE now (not at commit), so the log below
		// is written only when the database accepted it.
		employeeRepository.flush();
		logger.info("Employee updated: id={}, departmentId={}", id, department.getId());
		return toResponse(employee);
	}

	// Throws EmployeeNotFoundException (-> 404) when the id does not exist
	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public void deleteEmployee(Long id) {
		logger.debug("Deleting employee: id={}", id);
		employeeRepository.delete(findEmployee(id));
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

	// Lower case, so "An@Example.com" = "an@example.com" on MySQL and H2 (EmployeeRequest already trims)
	private String normalizeEmail(String email) {
		return email.toLowerCase();
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
