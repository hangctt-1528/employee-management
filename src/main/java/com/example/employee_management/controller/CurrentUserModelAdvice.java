package com.example.employee_management.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.employee_management.entity.AppUser;

// Adds ${currentUsername} and ${admin} to every page (header in fragments.html).
// Authentication is null when nobody is logged in. Hiding a link is cosmetic; SecurityConfig blocks access.
@ControllerAdvice
public class CurrentUserModelAdvice {

	@ModelAttribute("currentUsername")
	public String currentUsername(Authentication authentication) {
		if (authentication == null) {
			return null;
		}
		return authentication.getName();
	}

	@ModelAttribute("admin")
	public boolean admin(Authentication authentication) {
		if (authentication == null) {
			return false;
		}
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (("ROLE_" + AppUser.ADMIN).equals(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}

}
