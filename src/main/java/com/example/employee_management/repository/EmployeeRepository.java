package com.example.employee_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.employee_management.entity.Employee;

// A Pageable parameter makes Spring Data add LIMIT/OFFSET and run a second COUNT query for the totals.
// findAll(Pageable) is inherited from JpaRepository.
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	Page<Employee> findByNameContainingIgnoreCase(String name, Pageable pageable);

	Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

	Page<Employee> findByNameContainingIgnoreCaseAndDepartmentId(String name, Long departmentId, Pageable pageable);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

}
