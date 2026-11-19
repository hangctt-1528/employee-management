package com.example.employee_management.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.employee_management.entity.AppUser;
import com.example.employee_management.repository.AppUserRepository;
import com.example.employee_management.service.UserService;

// Loads a user from app_user for every login; Spring then checks the password with PasswordEncoder.
@Service
public class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository appUserRepository;

	public AppUserDetailsService(AppUserRepository appUserRepository) {
		this.appUserRepository = appUserRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUser user = appUserRepository.findByUsername(UserService.normalizeUsername(username));
		if (user == null) {
			// Same answer as a wrong password
			throw new UsernameNotFoundException("User not found");
		}
		// roles("ADMIN") -> ROLE_ADMIN
		return User.withUsername(user.getUsername())
				.password(user.getPassword())
				.roles(user.getRole())
				.build();
	}

}
