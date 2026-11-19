package com.example.employee_management;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.example.employee_management.scheduler.EmployeeCountCacheTask;

// The whole app on H2 with the real @Query: add employees through the API, then read the statistics
// (REST + page). DataSeeder created IT (id 1), HR (id 2), Sales (id 3).
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext // this test saves employees; give later tests a fresh database
class StatisticsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EmployeeCountCacheTask employeeCountCacheTask;

	private void addEmployee(String email, long departmentId) throws Exception {
		mockMvc.perform(post("/api/employees").with(httpBasic("admin", "admin123"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"an\",\"email\":\"" + email + "\",\"departmentId\":" + departmentId + "}"))
				.andExpect(status().isCreated());
	}

	@Test
	void statisticsFollowTheDataButTheCachedCountDoesNot() throws Exception {
		employeeCountCacheTask.clearEmployeeCountCache();

		// fresh database: the 3 seeded departments, all with 0 employees (LEFT JOIN)
		mockMvc.perform(get("/api/reports/statistics").with(httpBasic("user", "user1234")))
				.andExpect(jsonPath("$.totalEmployees").value(0))
				.andExpect(jsonPath("$.departments.length()").value(3))
				.andExpect(jsonPath("$.departments[0].employeeCount").value(0))
				.andExpect(jsonPath("$.departments[1].employeeCount").value(0))
				.andExpect(jsonPath("$.departments[2].employeeCount").value(0));

		// fill the Lab 8 cache with 0
		mockMvc.perform(get("/api/reports/employee-count").with(httpBasic("user", "user1234")))
				.andExpect(jsonPath("$.totalEmployees").value(0));

		addEmployee("a@example.com", 1);
		addEmployee("b@example.com", 1);
		addEmployee("c@example.com", 2);

		// statistics are not cached: they change at once
		mockMvc.perform(get("/api/reports/statistics").with(httpBasic("user", "user1234")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalEmployees").value(3))
				.andExpect(jsonPath("$.departments.length()").value(3))
				.andExpect(jsonPath("$.departments[0].departmentName").value("IT"))
				.andExpect(jsonPath("$.departments[0].employeeCount").value(2))
				.andExpect(jsonPath("$.departments[1].departmentName").value("HR"))
				.andExpect(jsonPath("$.departments[2].departmentName").value("Sales"))
				.andExpect(jsonPath("$.departments[2].employeeCount").value(0));

		mockMvc.perform(get("/api/reports/employees-by-department").with(httpBasic("user", "user1234")))
				.andExpect(jsonPath("$[0].employeeCount").value(2));

		// the Lab 8 count is cached up to 1 minute (by design)
		mockMvc.perform(get("/api/reports/employee-count").with(httpBasic("user", "user1234")))
				.andExpect(jsonPath("$.totalEmployees").value(0));

		// the page shows the same numbers (USER can see it; web pages use a session, not HTTP Basic)
		mockMvc.perform(get("/employees/statistics").with(user("user").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Total employees: <b>3</b>")));
	}

}
