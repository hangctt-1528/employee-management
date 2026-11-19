package com.example.employee_management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.employee_management.config.JwtConfig;
import com.example.employee_management.config.SecurityConfig;
import com.example.employee_management.dto.RegisterForm;
import com.example.employee_management.dto.UserResponse;
import com.example.employee_management.exception.DuplicateUsernameException;
import com.example.employee_management.service.UserService;

// Login, register and access-denied pages. Nobody is logged in unless a test says so.
@WebMvcTest(AuthWebController.class)
@Import({ SecurityConfig.class, JwtConfig.class })
class AuthWebControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	// ---- /login ----

	@Test
	void loginPageIsOpenAndHasTheCsrfField() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/login"))
				.andExpect(content().string(containsString("name=\"_csrf\"")))
				.andExpect(content().string(containsString("name=\"username\"")))
				.andExpect(content().string(containsString("Log in")))
				.andExpect(content().string(not(containsString("Log out")))); // header for a visitor
	}

	@Test
	void loginPageShowsMessages() throws Exception {
		mockMvc.perform(get("/login").param("error", ""))
				.andExpect(content().string(containsString("Invalid username or password.")));
		mockMvc.perform(get("/login").param("logout", ""))
				.andExpect(content().string(containsString("You have been logged out.")));
		mockMvc.perform(get("/login").param("registered", ""))
				.andExpect(content().string(containsString("Account created. Please log in.")));
	}

	// ---- /register ----

	@Test
	void registerPageShowsAnEmptyForm() throws Exception {
		mockMvc.perform(get("/register"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/register"))
				.andExpect(model().attributeExists("registerForm"));
	}

	@Test
	void validFormRegistersAndRedirectsToLogin() throws Exception {
		when(userService.register(any(RegisterForm.class))).thenReturn(new UserResponse(3L, "binh", "USER"));

		mockMvc.perform(post("/register").with(csrf())
						.param("username", "binh")
						.param("password", "binh12345")
						.param("confirmPassword", "binh12345"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?registered"));
		verify(userService).register(any(RegisterForm.class));
	}

	@Test
	void differentConfirmPasswordShowsError() throws Exception {
		mockMvc.perform(post("/register").with(csrf())
						.param("username", "binh")
						.param("password", "binh12345")
						.param("confirmPassword", "other12345"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/register"))
				.andExpect(model().attributeHasFieldErrorCode("registerForm", "confirmPassword", "mismatch"))
				.andExpect(content().string(containsString("Passwords do not match")));
		verify(userService, never()).register(any(RegisterForm.class));
	}

	@Test
	void invalidUsernameAndShortPasswordShowErrors() throws Exception {
		mockMvc.perform(post("/register").with(csrf())
						.param("username", "a:b")
						.param("password", "1234567")
						.param("confirmPassword", "1234567"))
				.andExpect(view().name("auth/register"))
				.andExpect(model().attributeHasFieldErrorCode("registerForm", "username", "Pattern"))
				.andExpect(model().attributeHasFieldErrorCode("registerForm", "password", "Size"))
				.andExpect(content().string(containsString("Password must be 8 to 72 characters")));
		verify(userService, never()).register(any(RegisterForm.class));
	}

	@Test
	void usernameOf3CharactersIsAccepted() throws Exception {
		when(userService.register(any(RegisterForm.class))).thenReturn(new UserResponse(3L, "abc", "USER"));

		mockMvc.perform(post("/register").with(csrf())
						.param("username", "abc")
						.param("password", "12345678")
						.param("confirmPassword", "12345678"))
				.andExpect(redirectedUrl("/login?registered"));
	}

	// 25 x "ệ" = 25 characters (passes @Size) but 75 bytes (more than BCrypt accepts)
	@Test
	void passwordLongerThan72BytesShowsErrorInsteadOf500() throws Exception {
		String password = "ệ".repeat(25);

		mockMvc.perform(post("/register").with(csrf())
						.param("username", "binh")
						.param("password", password)
						.param("confirmPassword", password))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/register"))
				.andExpect(model().attributeHasFieldErrorCode("registerForm", "password", "tooLong"));
		verify(userService, never()).register(any(RegisterForm.class));
	}

	@Test
	void passwordOf72BytesIsAccepted() throws Exception {
		when(userService.register(any(RegisterForm.class))).thenReturn(new UserResponse(3L, "binh", "USER"));
		String password = "ệ".repeat(24); // 72 bytes

		mockMvc.perform(post("/register").with(csrf())
						.param("username", "binh")
						.param("password", password)
						.param("confirmPassword", password))
				.andExpect(redirectedUrl("/login?registered"));
	}

	@Test
	void usernameOf2CharactersIsRejected() throws Exception {
		mockMvc.perform(post("/register").with(csrf())
						.param("username", "ab")
						.param("password", "12345678")
						.param("confirmPassword", "12345678"))
				.andExpect(model().attributeHasFieldErrorCode("registerForm", "username", "Size"));
	}

	@Test
	void takenUsernameIsShownOnTheUsernameField() throws Exception {
		when(userService.register(any(RegisterForm.class))).thenThrow(new DuplicateUsernameException("admin"));

		mockMvc.perform(post("/register").with(csrf())
						.param("username", "admin")
						.param("password", "admin12345")
						.param("confirmPassword", "admin12345"))
				.andExpect(view().name("auth/register"))
				.andExpect(model().attributeHasFieldErrorCode("registerForm", "username", "duplicate"))
				.andExpect(content().string(containsString("Username admin is already taken")));
	}

	@Test
	void registerWithoutCsrfTokenIsRejected() throws Exception {
		mockMvc.perform(post("/register")
						.param("username", "binh")
						.param("password", "binh12345")
						.param("confirmPassword", "binh12345"))
				.andExpect(status().isForbidden());
		verify(userService, never()).register(any(RegisterForm.class));
	}

	// ---- /access-denied ----

	@Test
	@WithMockUser
	void accessDeniedPageReturns403() throws Exception {
		mockMvc.perform(get("/access-denied"))
				.andExpect(status().isForbidden())
				.andExpect(view().name("error"))
				.andExpect(content().string(containsString("You do not have permission to do this")));
	}

}
