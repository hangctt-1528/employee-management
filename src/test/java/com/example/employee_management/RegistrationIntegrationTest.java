package com.example.employee_management;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.example.employee_management.entity.AppUser;
import com.example.employee_management.repository.AppUserRepository;

// The whole app: register on the /register page, then log in with the new account.
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext // this test saves a user; give later tests a fresh database
class RegistrationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AppUserRepository appUserRepository;

	@Test
	void registerThenLogInAsUser() throws Exception {
		mockMvc.perform(post("/register").with(csrf())
						.param("username", "Binh")
						.param("password", "binh12345")
						.param("confirmPassword", "binh12345"))
				.andExpect(redirectedUrl("/login?registered"));

		AppUser saved = appUserRepository.findByUsername("binh"); // stored in lower case
		assertEquals(AppUser.USER, saved.getRole());
		assertTrue(saved.getPassword().startsWith("$2a$")); // BCrypt hash, not the password

		// login page
		mockMvc.perform(formLogin("/login").user("binh").password("binh12345"))
				.andExpect(authenticated().withUsername("binh").withRoles("USER"));

		// the same account works for the API, with the USER rights only
		mockMvc.perform(get("/api/employees").with(httpBasic("binh", "binh12345")))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/employees/1").with(httpBasic("binh", "binh12345")))
				.andExpect(status().isForbidden());

		// the same username again (other case) -> error under the Username field
		mockMvc.perform(post("/register").with(csrf())
						.param("username", "BINH")
						.param("password", "other12345")
						.param("confirmPassword", "other12345"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Username binh is already taken")));
	}

}
