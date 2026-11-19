package com.example.employee_management.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.employee_management.config.JwtConfig;
import com.example.employee_management.config.SecurityConfig;
import com.example.employee_management.dto.DepartmentResponse;
import com.example.employee_management.service.DepartmentService;

@WebMvcTest(DepartmentController.class)
@Import({ SecurityConfig.class, JwtConfig.class }) // Lab 9: our security rules, not the default ones
@WithMockUser(roles = "ADMIN") // every request runs as a logged-in ADMIN unless a test says otherwise
class DepartmentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DepartmentService departmentService;

	@Test
	void getDepartmentsReturnsJsonList() throws Exception {
		List<DepartmentResponse> departments = new ArrayList<>();
		departments.add(new DepartmentResponse(1L, "IT"));
		departments.add(new DepartmentResponse(2L, "HR"));
		when(departmentService.getAllDepartments()).thenReturn(departments);

		mockMvc.perform(get("/api/departments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].name").value("IT"));
	}

}
