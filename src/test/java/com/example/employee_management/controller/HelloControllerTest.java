package com.example.employee_management.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.employee_management.config.JwtConfig;
import com.example.employee_management.config.SecurityConfig;

@WebMvcTest(HelloController.class)
@Import({ SecurityConfig.class, JwtConfig.class }) // Lab 9: our security rules, not the default ones
@WithMockUser // a logged-in USER (/hello itself is open to everyone)
class HelloControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void helloReturnsGreeting() throws Exception {
		mockMvc.perform(get("/hello"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
				.andExpect(content().string(HelloController.GREETING));
	}

	@Test
	void postHelloIsNotAllowed() throws Exception {
		mockMvc.perform(post("/hello").with(csrf()))
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	void unknownPathReturnsNotFound() throws Exception {
		mockMvc.perform(get("/hellox"))
				.andExpect(status().isNotFound());
	}

}
