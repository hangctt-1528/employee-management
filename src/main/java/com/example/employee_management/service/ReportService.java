package com.example.employee_management.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.employee_management.dto.EmployeeCountResponse;
import com.example.employee_management.repository.EmployeeRepository;

// Reports about employees (Lab 8: total number of employees).
// Call these methods from another bean: a call from inside this class does not go through
// the Spring proxy, so the cache would be skipped.
@Service
public class ReportService {

	public static final String EMPLOYEE_COUNT_CACHE = "employeeCount";

	private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

	private final EmployeeRepository employeeRepository;

	public ReportService(EmployeeRepository employeeRepository) {
		this.employeeRepository = employeeRepository;
	}

	// Cached in "employeeCount". While the value is in the cache, Spring returns it without running
	// this method (no database query). EmployeeCountCacheTask clears the cache every 60 s, so the value
	// is at most 1 minute old. Adding or deleting employees does not clear it (by design).
	@Cacheable(EMPLOYEE_COUNT_CACHE)
	public EmployeeCountResponse getEmployeeCount() {
		logger.debug("Counting employees in the database");
		long total = employeeRepository.count();
		return new EmployeeCountResponse(total, LocalDateTime.now());
	}

	// @CacheEvict removes every entry of the cache after this method returns
	@CacheEvict(cacheNames = EMPLOYEE_COUNT_CACHE, allEntries = true)
	public void clearEmployeeCountCache() {
		logger.debug("Employee count cache cleared");
	}

}
