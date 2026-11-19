package com.example.employee_management.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.employee_management.entity.AppUser;

@DataJpaTest
class AppUserRepositoryTest {

	@Autowired
	private AppUserRepository appUserRepository;

	@Test
	void saveCreatesTheAppUserTable() {
		AppUser saved = appUserRepository.save(new AppUser("binh", "hash", AppUser.USER));

		assertNotNull(saved.getId());
	}

	@Test
	void findByUsernameReturnsTheUserOrNull() {
		appUserRepository.save(new AppUser("binh", "hash", AppUser.USER));

		assertEquals(AppUser.USER, appUserRepository.findByUsername("binh").getRole());
		assertNull(appUserRepository.findByUsername("nobody"));
	}

	@Test
	void existsByUsername() {
		appUserRepository.save(new AppUser("binh", "hash", AppUser.USER));

		assertTrue(appUserRepository.existsByUsername("binh"));
		assertFalse(appUserRepository.existsByUsername("nobody"));
	}

	@Test
	void usernameIsUnique() {
		appUserRepository.saveAndFlush(new AppUser("binh", "hash", AppUser.USER));

		assertThrows(DataIntegrityViolationException.class,
				() -> appUserRepository.saveAndFlush(new AppUser("binh", "hash2", AppUser.USER)));
	}

}
