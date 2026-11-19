package com.example.employee_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.employee_management.entity.Employee;

// Spring Data JPA writes the implementation. findAll, findById, save, existsById, deleteById
// come from JpaRepository; the methods below are "query methods": Spring builds the SQL from the name.
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	// ... WHERE LOWER(name) LIKE LOWER('%keyword%')
	List<Employee> findByNameContainingIgnoreCase(String name);

	// ... WHERE department_id = ?
	List<Employee> findByDepartmentId(Long departmentId);

	// both conditions joined with AND
	List<Employee> findByNameContainingIgnoreCaseAndDepartmentId(String name, Long departmentId);

	boolean existsByEmail(String email);

	// used by update: is the email taken by ANOTHER employee?
	boolean existsByEmailAndIdNot(String email, Long id);

}
