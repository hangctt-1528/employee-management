package com.example.employee_management.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import com.example.employee_management.dto.RegisterForm;
import com.example.employee_management.exception.DuplicateUsernameException;
import com.example.employee_management.service.UserService;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

// Login, register and access-denied pages.
@Controller
public class AuthWebController {

	public static final String ACCESS_DENIED_MESSAGE = "You do not have permission to do this";
	public static final int MAX_PASSWORD_BYTES = 72; // limit of BCrypt

	private final UserService userService;

	public AuthWebController(UserService userService) {
		this.userService = userService;
	}

	// POST /login is handled by Spring Security
	@GetMapping("/login")
	public String showLoginPage() {
		return "auth/login";
	}

	@GetMapping("/register")
	public String showRegisterForm(Model model) {
		model.addAttribute("registerForm", new RegisterForm());
		return "auth/register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("registerForm") RegisterForm form, BindingResult bindingResult) {
		if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
			bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
		}
		// @Size counts characters, BCrypt allows 72 bytes ("ệ" = 3 bytes)
		if (form.getPassword() != null
				&& form.getPassword().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
			bindingResult.rejectValue("password", "tooLong",
					"Password is too long (at most 72 bytes; letters with accents count as 2 or 3)");
		}
		if (bindingResult.hasErrors()) {
			return "auth/register";
		}
		try {
			userService.register(form); // always a USER account
			return "redirect:/login?registered"; // no automatic login
		} catch (DuplicateUsernameException ex) {
			bindingResult.rejectValue("username", "duplicate", ex.getMessage());
			return "auth/register";
		}
	}

	// Spring Security forwards here (403) when the role is not allowed
	@GetMapping("/access-denied")
	public ModelAndView accessDenied(HttpServletRequest request) {
		String path = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
		if (path == null) {
			path = request.getRequestURI(); // opened directly
		}
		ModelAndView modelAndView = new ModelAndView("error");
		modelAndView.setStatus(HttpStatus.FORBIDDEN);
		modelAndView.addObject("status", HttpStatus.FORBIDDEN.value());
		modelAndView.addObject("error", HttpStatus.FORBIDDEN.getReasonPhrase());
		modelAndView.addObject("message", ACCESS_DENIED_MESSAGE);
		modelAndView.addObject("path", path);
		return modelAndView;
	}

}
