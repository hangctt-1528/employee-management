package com.example.employee_management.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.employee_management.entity.AppUser;
import com.example.employee_management.repository.AppUserRepository;

class AppUserDetailsServiceTest {

	private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
	private final AppUserDetailsService service = new AppUserDetailsService(appUserRepository);

	private List<String> authorities(UserDetails details) {
		List<String> names = new ArrayList<>();
		for (GrantedAuthority authority : details.getAuthorities()) {
			names.add(authority.getAuthority());
		}
		return names;
	}

	@Test
	void adminGetsRoleAdminAndTheUsernameIsNormalized() {
		when(appUserRepository.findByUsername("admin")).thenReturn(new AppUser("admin", "$2a$hash", AppUser.ADMIN));

		UserDetails details = service.loadUserByUsername(" Admin ");

		assertEquals("admin", details.getUsername());
		assertEquals("$2a$hash", details.getPassword());
		assertTrue(authorities(details).contains("ROLE_ADMIN"));
	}

	@Test
	void userGetsOnlyRoleUser() {
		when(appUserRepository.findByUsername("user")).thenReturn(new AppUser("user", "$2a$hash", AppUser.USER));

		List<String> names = authorities(service.loadUserByUsername("user"));

		assertTrue(names.contains("ROLE_USER"));
		assertFalse(names.contains("ROLE_ADMIN"));
	}

	@Test
	void unknownUserThrows() {
		assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nobody"));
	}

}
