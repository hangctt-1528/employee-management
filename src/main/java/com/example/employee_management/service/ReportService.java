package com.example.employee_management.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.employee_management.dto.DepartmentEmployeeCount;
import com.example.employee_management.dto.EmployeeCountResponse;
import com.example.employee_management.dto.EmployeeStatisticsResponse;
import com.example.employee_management.repository.DepartmentRepository;
import com.example.employee_management.repository.EmployeeRepository;

// Reports: total (cached) and statistics by department (not cached). Read-only transactions.
@Service
@Transactional(readOnly = true)
public class ReportService {

	public static final String EMPLOYEE_COUNT_CACHE = "employeeCount";

	private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

	private final EmployeeRepository employeeRepository;
	private final DepartmentRepository departmentRepository;

	public ReportService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
		this.employeeRepository = employeeRepository;
		this.departmentRepository = departmentRepository;
	}

	// Cached in "employeeCount": while the value is cached, this method does not run (no query).
	// EmployeeCountCacheTask clears the cache every 60 s, so the value is at most 1 minute old.
	// Adding or deleting employees does not clear it (by design).
	@Cacheable(EMPLOYEE_COUNT_CACHE)
	public EmployeeCountResponse getEmployeeCount() {
		logger.debug("Counting employees in the database");
		return new EmployeeCountResponse(employeeRepository.count(), LocalDateTime.now());
	}

	// Not cached
	public List<DepartmentEmployeeCount> getEmployeesByDepartment() {
		return departmentRepository.countEmployeesByDepartment();
	}

	// Not cached; one transaction, so the total equals the sum of the rows
	public EmployeeStatisticsResponse getStatistics() {
		long total = employeeRepository.count();
		List<DepartmentEmployeeCount> departments = departmentRepository.countEmployeesByDepartment();
		logger.debug("Statistics computed: totalEmployees={}, departments={}", total, departments.size());
		return new EmployeeStatisticsResponse(total, departments, LocalDateTime.now());
	}

}
