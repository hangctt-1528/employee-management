package com.example.employee_management.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.employee_management.service.UtilityService;

// Starts the real IoC container to check that both beans are registered.
@SpringBootTest
class AppConfigTest {

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UtilityService utilityService;

	@Test
	void beansAreInTheContainer() {
		assertNotNull(passwordEncoder);
		assertNotNull(utilityService);
	}

	@Test
	void passwordEncoderHashesAndMatches() {
		String hash = passwordEncoder.encode("123456");

		assertNotEquals("123456", hash);
		assertTrue(hash.startsWith("$2a$"));
		assertTrue(passwordEncoder.matches("123456", hash));
		assertFalse(passwordEncoder.matches("wrong", hash));
	}

}
