package com.example.employee_management.service;

import static com.example.employee_management.LogTestHelper.hasLine;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.employee_management.dto.RegisterForm;
import com.example.employee_management.entity.AppUser;
import com.example.employee_management.exception.DuplicateUsernameException;
import com.example.employee_management.repository.AppUserRepository;

// UserService with the real repository (H2) and a real BCrypt PasswordEncoder
@DataJpaTest
@Import({ UserService.class, BCryptPasswordEncoder.class })
@ExtendWith(OutputCaptureExtension.class)
class UserServiceTest {

	@Autowired
	private UserService userService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private RegisterForm form(String username, String password) {
		return new RegisterForm(username, password, password);
	}

	@Test
	void registerCreatesAUserWithAHashedPassword() {
		userService.register(form("  Binh ", "binh12345"));

		AppUser saved = appUserRepository.findByUsername("binh"); // trimmed + lower case
		assertEquals(AppUser.USER, saved.getRole());
		String hash = saved.getPassword();
		assertNotEquals("binh12345", hash);
		assertTrue(hash.startsWith("$2a$")); // BCrypt
		assertTrue(passwordEncoder.matches("binh12345", hash));
	}

	@Test
	void usernameIsCaseInsensitive() {
		userService.register(form("binh", "binh12345"));

		assertThrows(DuplicateUsernameException.class, () -> userService.register(form("BINH", "other12345")));
		assertEquals(1, appUserRepository.count());
	}

	@Test
	void getUserNormalizesTheUsername() {
		userService.register(form("binh", "binh12345"));

		assertEquals("binh", userService.getUser(" Binh ").getUsername());
	}

	@Test
	void getUnknownUserThrows() {
		assertThrows(UsernameNotFoundException.class, () -> userService.getUser("nobody"));
	}

	@Test
	void registerLogsOnlyIdAndRole(CapturedOutput output) {
		userService.register(form("binh", "binh12345"));

		String log = output.getOut();
		assertTrue(hasLine(log, "INFO", "User registered: id="));
		assertFalse(log.contains("binh12345")); // never the password
		assertFalse(log.contains("$2a$")); // never the hash
	}

	@Test
	void duplicateUsernameLogsWarn(CapturedOutput output) {
		userService.register(form("binh", "binh12345"));

		assertThrows(DuplicateUsernameException.class, () -> userService.register(form("binh", "binh12345")));

		assertTrue(hasLine(output.getOut(), "WARN", "Cannot register user: username is already used"));
	}

}
