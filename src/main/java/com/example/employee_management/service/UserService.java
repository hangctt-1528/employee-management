package com.example.employee_management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.employee_management.dto.RegisterForm;
import com.example.employee_management.dto.UserResponse;
import com.example.employee_management.entity.AppUser;
import com.example.employee_management.exception.DuplicateUsernameException;
import com.example.employee_management.repository.AppUserRepository;

@Service
public class UserService {

	// Logs only id and role
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	// "Binh" and "binh" are the same account
	public static String normalizeUsername(String username) {
		if (username == null) {
			return null;
		}
		return username.trim().toLowerCase();
	}

	// Always role USER; DuplicateUsernameException if taken
	public UserResponse register(RegisterForm form) {
		String username = normalizeUsername(form.getUsername());
		if (appUserRepository.existsByUsername(username)) {
			logger.warn("Cannot register user: username is already used");
			throw new DuplicateUsernameException(username);
		}
		String hash = passwordEncoder.encode(form.getPassword()); // BCrypt
		AppUser saved = appUserRepository.save(new AppUser(username, hash, AppUser.USER));
		logger.info("User registered: id={}, role={}", saved.getId(), saved.getRole());
		return toResponse(saved);
	}

	// UsernameNotFoundException (-> 401) if the account was removed
	public UserResponse getUser(String username) {
		AppUser user = appUserRepository.findByUsername(normalizeUsername(username));
		if (user == null) {
			throw new UsernameNotFoundException("User not found");
		}
		return toResponse(user);
	}

	private UserResponse toResponse(AppUser user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getRole());
	}

}
