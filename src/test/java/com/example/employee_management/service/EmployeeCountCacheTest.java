package com.example.employee_management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import com.example.employee_management.dto.EmployeeCountResponse;
import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.entity.Department;
import com.example.employee_management.repository.DepartmentRepository;
import com.example.employee_management.scheduler.EmployeeCountCacheTask;

// The whole app with the real cache (@EnableCaching in AppConfig + spring.cache.* in application.yml).
// ReportService here is the Spring proxy, so @Cacheable / @CacheEvict work.
@SpringBootTest
@DirtiesContext // this test saves an employee; give later tests a fresh database
@WithMockUser(roles = "ADMIN") // Lab 9: adding employees needs the ADMIN role
class EmployeeCountCacheTest {

	@Autowired
	private ReportService reportService;

	@Autowired
	private EmployeeCountCacheTask employeeCountCacheTask;

	@Autowired
	private EmployeeService employeeService;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Test
	void countIsCachedUntilTheCacheIsCleared() {
		employeeCountCacheTask.clearEmployeeCountCache(); // start from an empty cache
		Department department = departmentRepository.findAll().get(0); // created by DataSeeder

		EmployeeCountResponse first = reportService.getEmployeeCount();
		employeeService.addEmployee(new EmployeeRequest("an", "cache.an@example.com", department.getId()));
		EmployeeCountResponse second = reportService.getEmployeeCount();

		// cache hit: same object, the new employee is not counted yet
		assertEquals(first.getTotalEmployees(), second.getTotalEmployees());
		assertEquals(first.getCountedAt(), second.getCountedAt());

		employeeCountCacheTask.clearEmployeeCountCache(); // what the scheduler does every minute
		EmployeeCountResponse third = reportService.getEmployeeCount();

		assertEquals(first.getTotalEmployees() + 1, third.getTotalEmployees());
	}

}
