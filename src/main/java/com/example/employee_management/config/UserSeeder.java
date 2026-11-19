package com.example.employee_management.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.employee_management.entity.AppUser;
import com.example.employee_management.repository.AppUserRepository;

// Creates the accounts "admin" (ADMIN) and "user" (USER) if missing.
// Separate from DataSeeder, which stops when departments already exist.
@Component
public class UserSeeder implements CommandLineRunner {

	public static final String ADMIN_USERNAME = "admin";
	public static final String USER_USERNAME = "user";
	public static final int MIN_PASSWORD_LENGTH = 8;

	private static final Logger logger = LoggerFactory.getLogger(UserSeeder.class);

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final String adminPassword;
	private final String userPassword;

	// Passwords from APP_ADMIN_PASSWORD / APP_USER_PASSWORD
	public UserSeeder(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
			@Value("${app.security.admin-password}") String adminPassword,
			@Value("${app.security.user-password}") String userPassword) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.adminPassword = adminPassword;
		this.userPassword = userPassword;
	}

	@Override
	public void run(String... args) {
		createIfMissing(ADMIN_USERNAME, adminPassword, AppUser.ADMIN);
		createIfMissing(USER_USERNAME, userPassword, AppUser.USER);
	}

	// An existing account is never changed
	private void createIfMissing(String username, String rawPassword, String role) {
		if (appUserRepository.existsByUsername(username)) {
			return;
		}
		if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
			throw new IllegalStateException("The password of the default " + role + " account must have at least "
					+ MIN_PASSWORD_LENGTH + " characters (APP_ADMIN_PASSWORD / APP_USER_PASSWORD)");
		}
		AppUser saved = appUserRepository.save(new AppUser(username, passwordEncoder.encode(rawPassword), role));
		logger.info("Default account created: id={}, role={}", saved.getId(), role);
	}

}
