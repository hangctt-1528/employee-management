package com.example.employee_management.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.employee_management.config.JwtConfig;
import com.example.employee_management.config.SecurityConfig;
import com.example.employee_management.dto.UserResponse;
import com.example.employee_management.security.JwtTokenService;
import com.example.employee_management.service.UserService;

// The HTTP layer of /api/auth: the AuthenticationManager is a mock, JwtTokenService is the real one.
// The full login flow with real accounts is in SecurityRulesTest.
@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtConfig.class, JwtTokenService.class })
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	@MockitoBean
	private UserService userService;

	private UsernamePasswordAuthenticationToken loggedIn(String username, String role) {
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
		return new UsernamePasswordAuthenticationToken(username, null, authorities);
	}

	@Test
	void loginReturnsABearerToken() throws Exception {
		when(authenticationManager.authenticate(any())).thenReturn(loggedIn("admin", "ADMIN"));

		mockMvc.perform(post("/api/auth/login") // no login and no CSRF token needed
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600))
				.andExpect(jsonPath("$.accessToken", startsWith("eyJ"))); // Base64 of {"alg":...
	}

	@Test
	void wrongPasswordReturns401() throws Exception {
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	void blankFieldsReturn400() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"\",\"password\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.username").value("Username must not be blank"))
				.andExpect(jsonPath("$.fieldErrors.password").value("Password must not be blank"));
	}

	@Test
	void meWithoutLoginReturns401() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.path").value("/api/auth/me"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void meReturnsTheLoggedInUser() throws Exception {
		when(userService.getUser("admin")).thenReturn(new UserResponse(1L, "admin", "ADMIN"));

		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("admin"))
				.andExpect(jsonPath("$.role").value("ADMIN"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

}
