package com.example.employee_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.employee_management.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	// null if not found; pass a lower-case username
	AppUser findByUsername(String username);

	boolean existsByUsername(String username);

}
