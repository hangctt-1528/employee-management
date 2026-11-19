package com.example.employee_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.employee_management.dto.DepartmentEmployeeCount;
import com.example.employee_management.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

	// JPQL (runs on MySQL and H2); LEFT JOIN keeps departments with 0 employees
	@Query("select new com.example.employee_management.dto.DepartmentEmployeeCount(d.id, d.name, count(e)) "
			+ "from Department d left join Employee e on e.department = d "
			+ "group by d.id, d.name "
			+ "order by count(e) desc, d.name asc")
	List<DepartmentEmployeeCount> countEmployeesByDepartment();

}
