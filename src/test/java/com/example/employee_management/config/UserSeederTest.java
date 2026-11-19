package com.example.employee_management.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.employee_management.entity.AppUser;
import com.example.employee_management.repository.AppUserRepository;

// The whole app: UserSeeder ran at startup with the passwords of src/test/resources/config/application.yml
@SpringBootTest
class UserSeederTest {

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserSeeder userSeeder;

	@Test
	void adminAccountIsCreated() {
		AppUser admin = appUserRepository.findByUsername("admin");

		assertEquals(AppUser.ADMIN, admin.getRole());
		assertTrue(passwordEncoder.matches("admin123", admin.getPassword()));
	}

	@Test
	void userAccountIsCreated() {
		AppUser user = appUserRepository.findByUsername("user");

		assertEquals(AppUser.USER, user.getRole());
		assertTrue(passwordEncoder.matches("user1234", user.getPassword()));
	}

	@Test
	void runningAgainChangesNothing() {
		long count = appUserRepository.count();
		String adminHash = appUserRepository.findByUsername("admin").getPassword();

		userSeeder.run();

		assertEquals(count, appUserRepository.count());
		assertEquals(adminHash, appUserRepository.findByUsername("admin").getPassword());
	}

	// A mock repository with no accounts (existsByUsername -> false), so the seeder must create them
	@Test
	void passwordShorterThan8CharactersStopsTheApp() {
		AppUserRepository emptyRepository = mock(AppUserRepository.class);
		UserSeeder seeder = new UserSeeder(emptyRepository, passwordEncoder, "1234567", "user1234");

		assertThrows(IllegalStateException.class, () -> seeder.run());
	}

	@Test
	void passwordOf8CharactersIsAccepted() {
		AppUserRepository emptyRepository = mock(AppUserRepository.class);
		when(emptyRepository.save(any(AppUser.class))).thenReturn(new AppUser("x", "hash", AppUser.USER));
		UserSeeder seeder = new UserSeeder(emptyRepository, passwordEncoder, "12345678", "user1234");

		seeder.run();

		verify(emptyRepository, times(2)).save(any(AppUser.class));
	}

}
