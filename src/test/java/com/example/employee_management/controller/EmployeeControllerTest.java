package com.example.employee_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

	private static final String VALID_BODY = "{\"name\":\"nguyen van an\",\"email\":\"an@example.com\",\"departmentId\":1}";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmployeeService employeeService;

	private EmployeeResponse sample() {
		return new EmployeeResponse(1L, "Nguyen Van An", "an@example.com", 1L, "IT");
	}

	@Test
	void getEmployeesPassesBothParamsToSearch() throws Exception {
		List<EmployeeResponse> found = new ArrayList<>();
		found.add(sample());
		when(employeeService.searchEmployees("an", 1L)).thenReturn(found);

		mockMvc.perform(get("/api/employees").param("name", "an").param("departmentId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("Nguyen Van An"))
				.andExpect(jsonPath("$[0].departmentId").value(1))
				.andExpect(jsonPath("$[0].departmentName").value("IT"));
	}

	@Test
	void getEmployeesWithoutParamsSearchesWithNulls() throws Exception {
		when(employeeService.searchEmployees(null, null)).thenReturn(new ArrayList<>());

		mockMvc.perform(get("/api/employees"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		verify(employeeService).searchEmployees(null, null);
	}

	@Test
	void departmentIdThatIsNotANumberReturns400() throws Exception {
		mockMvc.perform(get("/api/employees").param("departmentId", "abc"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getEmployeeByIdReturnsEmployee() throws Exception {
		when(employeeService.getEmployeeById(1L)).thenReturn(sample());

		mockMvc.perform(get("/api/employees/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.email").value("an@example.com"));
	}

	@Test
	void getEmployeeByIdReturns404WhenMissing() throws Exception {
		when(employeeService.getEmployeeById(99L)).thenReturn(null);

		mockMvc.perform(get("/api/employees/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createEmployeeReturns201WithLocation() throws Exception {
		when(employeeService.departmentExists(1L)).thenReturn(true);
		when(employeeService.emailExists("an@example.com")).thenReturn(false);
		when(employeeService.addEmployee(any(EmployeeRequest.class))).thenReturn(sample());

		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/employees/1"))
				.andExpect(jsonPath("$.departmentName").value("IT"));
	}

	@Test
	void createEmployeeWithoutDepartmentReturns400() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"an\",\"email\":\"an@example.com\"}"))
				.andExpect(status().isBadRequest());
		verify(employeeService, never()).addEmployee(any(EmployeeRequest.class));
	}

	@Test
	void createEmployeeWithBlankNameReturns400() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\" \",\"email\":\"an@example.com\",\"departmentId\":1}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createEmployeeWithUnknownDepartmentReturns400() throws Exception {
		when(employeeService.departmentExists(1L)).thenReturn(false);

		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isBadRequest());
		verify(employeeService, never()).addEmployee(any(EmployeeRequest.class));
	}

	@Test
	void createEmployeeWithUsedEmailReturns409() throws Exception {
		when(employeeService.departmentExists(1L)).thenReturn(true);
		when(employeeService.emailExists("an@example.com")).thenReturn(true);

		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isConflict());
		verify(employeeService, never()).addEmployee(any(EmployeeRequest.class));
	}

	@Test
	void createEmployeeWithInvalidJsonReturns400() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("not json"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateEmployeeReturns200() throws Exception {
		when(employeeService.departmentExists(1L)).thenReturn(true);
		when(employeeService.employeeExists(1L)).thenReturn(true);
		when(employeeService.emailUsedByOther("an@example.com", 1L)).thenReturn(false);
		when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class))).thenReturn(sample());

		mockMvc.perform(put("/api/employees/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void updateMissingEmployeeReturns404() throws Exception {
		when(employeeService.departmentExists(1L)).thenReturn(true);
		when(employeeService.employeeExists(99L)).thenReturn(false);

		mockMvc.perform(put("/api/employees/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateWithEmailOfAnotherEmployeeReturns409() throws Exception {
		when(employeeService.departmentExists(1L)).thenReturn(true);
		when(employeeService.employeeExists(1L)).thenReturn(true);
		when(employeeService.emailUsedByOther("an@example.com", 1L)).thenReturn(true);

		mockMvc.perform(put("/api/employees/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isConflict());
		verify(employeeService, never()).updateEmployee(eq(1L), any(EmployeeRequest.class));
	}

	@Test
	void updateWithBlankEmailReturns400() throws Exception {
		mockMvc.perform(put("/api/employees/1").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"an\",\"email\":\"\",\"departmentId\":1}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deleteEmployeeReturns204() throws Exception {
		when(employeeService.deleteEmployee(1L)).thenReturn(true);

		mockMvc.perform(delete("/api/employees/1"))
				.andExpect(status().isNoContent());
	}

	@Test
	void deleteMissingEmployeeReturns404() throws Exception {
		when(employeeService.deleteEmployee(99L)).thenReturn(false);

		mockMvc.perform(delete("/api/employees/99"))
				.andExpect(status().isNotFound());
	}

}
