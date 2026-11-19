package com.example.employee_management;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

// The Actuator endpoints opened by management.endpoints.web.exposure.include
// (the copy in src/test/resources/application.yml).
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorEndpointsTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthIsUpWithDatabaseDetails() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components.db.status").value("UP")); // shown thanks to show-details: always
	}

	@Test
	void metricsListsJvmMemory() throws Exception {
		mockMvc.perform(get("/actuator/metrics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.names", hasItem("jvm.memory.used")));
	}

	@Test
	void oneMetricHasItsValue() throws Exception {
		mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("jvm.memory.used"))
				.andExpect(jsonPath("$.measurements[0].statistic").value("VALUE"));
	}

	@Test
	void cachesListsTheReportCache() throws Exception {
		mockMvc.perform(get("/actuator/caches"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cacheManagers.cacheManager.caches.employeeCount").exists());
	}

	@Test
	void scheduledTasksRunEvery30And60Seconds() throws Exception {
		mockMvc.perform(get("/actuator/scheduledtasks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fixedRate[*].runnable.target", hasItems(
						"com.example.employee_management.scheduler.SystemStatusTask.logSystemRunning",
						"com.example.employee_management.scheduler.EmployeeCountCacheTask.clearEmployeeCountCache")))
				.andExpect(jsonPath("$.fixedRate[*].interval", hasItems(30000, 60000)));
	}

	// Not in the exposure list -> our GlobalExceptionHandler answers 404
	@Test
	void endpointThatIsNotOpenReturns404() throws Exception {
		mockMvc.perform(get("/actuator/env"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("No endpoint at this URL"));
	}

}
