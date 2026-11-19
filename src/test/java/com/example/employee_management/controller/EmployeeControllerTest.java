package com.example.employee_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.service.EmployeeService;

// Tests only the HTTP layer; the service is replaced by a Mockito mock.
@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmployeeService employeeService;

	@Test
	void getEmployeesReturnsJsonList() throws Exception {
		List<EmployeeResponse> employees = new ArrayList<>();
		employees.add(new EmployeeResponse(1L, "EMP0001", "Nguyen Van An", "an@example.com"));
		when(employeeService.getAllEmployees()).thenReturn(employees);

		mockMvc.perform(get("/api/employees"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].code").value("EMP0001"))
				.andExpect(jsonPath("$[0].name").value("Nguyen Van An"))
				.andExpect(jsonPath("$[0].email").value("an@example.com"));
	}

	@Test
	void getEmployeesWhenEmptyReturnsEmptyArray() throws Exception {
		when(employeeService.getAllEmployees()).thenReturn(new ArrayList<>());

		mockMvc.perform(get("/api/employees"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void getEmployeesWithNameParamSearches() throws Exception {
		List<EmployeeResponse> found = new ArrayList<>();
		found.add(new EmployeeResponse(1L, "EMP0001", "Nguyen Van An", "an@example.com"));
		when(employeeService.searchByName("an")).thenReturn(found);

		mockMvc.perform(get("/api/employees").param("name", "an"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Nguyen Van An"));
		verify(employeeService).searchByName("an");
	}

	@Test
	void getEmployeeByIdReturnsEmployee() throws Exception {
		when(employeeService.getEmployeeById(1L))
				.thenReturn(new EmployeeResponse(1L, "EMP0001", "Nguyen Van An", "an@example.com"));

		mockMvc.perform(get("/api/employees/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("EMP0001"));
	}

	@Test
	void getEmployeeByIdReturns404WhenMissing() throws Exception {
		when(employeeService.getEmployeeById(99L)).thenReturn(null);

		mockMvc.perform(get("/api/employees/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getEmployeeByIdReturns400WhenIdIsNotANumber() throws Exception {
		mockMvc.perform(get("/api/employees/abc"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createEmployeeReturns201WithLocation() throws Exception {
		when(employeeService.addEmployee(any(EmployeeRequest.class)))
				.thenReturn(new EmployeeResponse(1L, "EMP0001", "Nguyen Van An", "an@example.com"));

		mockMvc.perform(post("/api/employees")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"nguyen van an\",\"email\":\"an@example.com\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/employees/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Nguyen Van An"));
	}

	@Test
	void createEmployeeWithBlankNameReturns400() throws Exception {
		mockMvc.perform(post("/api/employees")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"  \",\"email\":\"an@example.com\"}"))
				.andExpect(status().isBadRequest());
		verify(employeeService, never()).addEmployee(any(EmployeeRequest.class));
	}

	@Test
	void createEmployeeWithoutEmailReturns400() throws Exception {
		mockMvc.perform(post("/api/employees")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"an\"}"))
				.andExpect(status().isBadRequest());
		verify(employeeService, never()).addEmployee(any(EmployeeRequest.class));
	}

	@Test
	void createEmployeeWithoutJsonContentTypeReturns415() throws Exception {
		mockMvc.perform(post("/api/employees")
						.contentType(MediaType.TEXT_PLAIN)
						.content("name=an"))
				.andExpect(status().isUnsupportedMediaType());
	}

}
