package com.example.employee_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.employee_management.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	List<Employee> findByNameContainingIgnoreCase(String name);

	List<Employee> findByDepartmentId(Long departmentId);

	List<Employee> findByNameContainingIgnoreCaseAndDepartmentId(String name, Long departmentId);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

}
