package com.example.employee_management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

// The whole app on H2 (real services, repositories and templates):
// add an employee with the form, then see it on the list and search pages.
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext // this test saves an employee; give later tests a fresh database
class EmployeeWebPagesIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void addThenListThenSearch() throws Exception {
		// DataSeeder created IT (id 1), HR (id 2), Sales (id 3)
		mockMvc.perform(post("/employees/add")
						.param("name", "tran thi binh")
						.param("email", "binh@example.com")
						.param("departmentId", "2"))
				.andExpect(redirectedUrl("/employees/list"));

		mockMvc.perform(get("/employees/list"))
				.andExpect(content().string(containsString("Tran Thi Binh")))
				.andExpect(content().string(containsString("binh@example.com")));

		mockMvc.perform(get("/employees/search").param("name", "BINH").param("departmentId", "2"))
				.andExpect(content().string(containsString("Tran Thi Binh")));

		mockMvc.perform(get("/employees/search").param("name", "binh").param("departmentId", "1"))
				.andExpect(content().string(containsString("No employees found.")));
	}

}
