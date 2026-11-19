package com.example.employee_management.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.employee_management.config.JwtConfig;
import com.example.employee_management.config.SecurityConfig;
import com.example.employee_management.dto.DepartmentEmployeeCount;
import com.example.employee_management.dto.EmployeeCountResponse;
import com.example.employee_management.dto.EmployeeStatisticsResponse;
import com.example.employee_management.service.ReportService;

@WebMvcTest(ReportController.class)
@Import({ SecurityConfig.class, JwtConfig.class }) // Lab 9: our security rules, not the default ones
@WithMockUser(roles = "ADMIN") // every request runs as a logged-in ADMIN unless a test says otherwise
class ReportControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReportService reportService;

	@Test
	void employeeCountReturnsTotalAndTime() throws Exception {
		when(reportService.getEmployeeCount())
				.thenReturn(new EmployeeCountResponse(5, LocalDateTime.of(2026, 11, 19, 10, 15, 30)));

		mockMvc.perform(get("/api/reports/employee-count"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalEmployees").value(5))
				.andExpect(jsonPath("$.countedAt").value("2026-11-19T10:15:30"));
	}

	@Test
	void postIsNotAllowed() throws Exception {
		mockMvc.perform(post("/api/reports/employee-count"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405));
	}

	@Test
	void unexpectedErrorReturns500WithoutDetails() throws Exception {
		when(reportService.getEmployeeCount()).thenThrow(new RuntimeException("secret"));

		mockMvc.perform(get("/api/reports/employee-count"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").value("Unexpected error, please try again later"));
	}

	// ---- Lab 10 ----

	private List<DepartmentEmployeeCount> twoDepartments() {
		List<DepartmentEmployeeCount> rows = new ArrayList<>();
		rows.add(new DepartmentEmployeeCount(1L, "IT", 2L));
		rows.add(new DepartmentEmployeeCount(2L, "HR", 0L));
		return rows;
	}

	@Test
	void employeesByDepartmentReturnsTheRows() throws Exception {
		when(reportService.getEmployeesByDepartment()).thenReturn(twoDepartments());

		mockMvc.perform(get("/api/reports/employees-by-department"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].departmentId").value(1))
				.andExpect(jsonPath("$[0].departmentName").value("IT"))
				.andExpect(jsonPath("$[0].employeeCount").value(2))
				.andExpect(jsonPath("$[1].employeeCount").value(0));
	}

	@Test
	void statisticsReturnsTotalAndDepartments() throws Exception {
		when(reportService.getStatistics()).thenReturn(
				new EmployeeStatisticsResponse(2, twoDepartments(), LocalDateTime.of(2026, 11, 19, 10, 15, 30)));

		mockMvc.perform(get("/api/reports/statistics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalEmployees").value(2))
				.andExpect(jsonPath("$.departments.length()").value(2))
				.andExpect(jsonPath("$.departments[0].departmentName").value("IT"))
				.andExpect(jsonPath("$.generatedAt").value("2026-11-19T10:15:30"));
	}

	@Test
	@WithMockUser(roles = "USER")
	void userCanReadTheStatistics() throws Exception {
		when(reportService.getStatistics()).thenReturn(
				new EmployeeStatisticsResponse(0, new ArrayList<>(), LocalDateTime.now()));

		mockMvc.perform(get("/api/reports/statistics"))
				.andExpect(status().isOk());
	}

	// Only GET is open to USER; any other method falls to the "ADMIN only" rule
	@Test
	@WithMockUser(roles = "USER")
	void userCannotPostToStatistics() throws Exception {
		mockMvc.perform(post("/api/reports/statistics"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithAnonymousUser
	void statisticsNeedALogin() throws Exception {
		mockMvc.perform(get("/api/reports/statistics"))
				.andExpect(status().isUnauthorized());
	}

}
