package com.example.employee_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.example.employee_management.exception.DepartmentNotFoundException;
import com.example.employee_management.exception.DuplicateEmailException;
import com.example.employee_management.exception.EmployeeNotFoundException;
import com.example.employee_management.service.EmployeeService;

// Tests the HTTP layer: the controller, @Valid and GlobalExceptionHandler
// (@WebMvcTest also loads @ControllerAdvice classes). The service is a Mockito mock.
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

	// ---- success ----

	@Test
	void getEmployeesPassesBothParamsToSearch() throws Exception {
		List<EmployeeResponse> found = new ArrayList<>();
		found.add(sample());
		when(employeeService.searchEmployees("an", 1L)).thenReturn(found);

		mockMvc.perform(get("/api/employees").param("name", "an").param("departmentId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("Nguyen Van An"))
				.andExpect(jsonPath("$[0].departmentName").value("IT"));
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
	void createEmployeeReturns201WithLocation() throws Exception {
		when(employeeService.addEmployee(any(EmployeeRequest.class))).thenReturn(sample());

		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/employees/1"))
				.andExpect(jsonPath("$.departmentName").value("IT"));
	}

	@Test
	void updateEmployeeReturns200() throws Exception {
		when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class))).thenReturn(sample());

		mockMvc.perform(put("/api/employees/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void deleteEmployeeReturns204() throws Exception {
		mockMvc.perform(delete("/api/employees/1"))
				.andExpect(status().isNoContent());
		verify(employeeService).deleteEmployee(1L);
	}

	// ---- 404 not found (EmployeeNotFoundException) ----

	@Test
	void getMissingEmployeeReturns404WithMessage() throws Exception {
		when(employeeService.getEmployeeById(99L)).thenThrow(new EmployeeNotFoundException(99L));

		mockMvc.perform(get("/api/employees/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("Employee not found with id 99"))
				.andExpect(jsonPath("$.path").value("/api/employees/99"))
				.andExpect(jsonPath("$.timestamp").isString()) // LocalDateTime is written as an ISO string
				.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	@Test
	void updateMissingEmployeeReturns404() throws Exception {
		when(employeeService.updateEmployee(eq(99L), any(EmployeeRequest.class)))
				.thenThrow(new EmployeeNotFoundException(99L));

		mockMvc.perform(put("/api/employees/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Employee not found with id 99"));
	}

	@Test
	void deleteMissingEmployeeReturns404() throws Exception {
		doThrow(new EmployeeNotFoundException(99L)).when(employeeService).deleteEmployee(99L);

		mockMvc.perform(delete("/api/employees/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Employee not found with id 99"));
	}

	// ---- 400 validation (@Valid) ----

	@Test
	void blankNameReturns400WithFieldError() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"  \",\"email\":\"an@example.com\",\"departmentId\":1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request data is not valid"))
				.andExpect(jsonPath("$.fieldErrors.name").value("Name must not be blank"));
		verify(employeeService, never()).addEmployee(any(EmployeeRequest.class));
	}

	@Test
	void invalidEmailReturns400WithFieldError() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"an\",\"email\":\"not-an-email\",\"departmentId\":1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.email").value("Email is not valid"));
	}

	@Test
	void emailWithSpacesAroundIsTrimmedThenAccepted() throws Exception {
		when(employeeService.addEmployee(any(EmployeeRequest.class))).thenReturn(sample());

		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"an\",\"email\":\"  an@example.com  \",\"departmentId\":1}"))
				.andExpect(status().isCreated());
		verify(employeeService).addEmployee(argThat(request -> "an@example.com".equals(request.getEmail())));
	}

	@Test
	void emailWithOnlySpacesIsBlank() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"an\",\"email\":\"   \",\"departmentId\":1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.email").value("Email must not be blank"));
	}

	@Test
	void missingDepartmentIdReturns400WithFieldError() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"an\",\"email\":\"an@example.com\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.departmentId").value("Department id is required"));
	}

	@Test
	void tooLongNameReturns400() throws Exception {
		String longName = "a".repeat(101);
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + longName + "\",\"email\":\"an@example.com\",\"departmentId\":1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.name").value("Name must be at most 100 characters"));
	}

	@Test
	void severalInvalidFieldsAreAllListed() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.name").exists())
				.andExpect(jsonPath("$.fieldErrors.email").exists())
				.andExpect(jsonPath("$.fieldErrors.departmentId").exists());
	}

	@Test
	void putIsValidatedToo() throws Exception {
		mockMvc.perform(put("/api/employees/1").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"an\",\"email\":\"\",\"departmentId\":1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.email").exists());
		verify(employeeService, never()).updateEmployee(eq(1L), any(EmployeeRequest.class));
	}

	// ---- 400 / 409 from the service ----

	@Test
	void unknownDepartmentReturns400() throws Exception {
		when(employeeService.addEmployee(any(EmployeeRequest.class))).thenThrow(new DepartmentNotFoundException(1L));

		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Department not found with id 1"));
	}

	@Test
	void usedEmailReturns409() throws Exception {
		when(employeeService.addEmployee(any(EmployeeRequest.class)))
				.thenThrow(new DuplicateEmailException("an@example.com"));

		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("Email an@example.com is already used by another employee"));
	}

	// ---- 400 wrong data format ----

	@Test
	void brokenJsonReturns400WithClearMessage() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{\"name\": "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"Request body is missing or is not valid JSON (check the syntax and the data types, e.g. departmentId must be a number)"));
	}

	@Test
	void wrongTypeInBodyReturns400() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"an\",\"email\":\"an@example.com\",\"departmentId\":\"abc\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void idThatIsNotANumberReturns400() throws Exception {
		mockMvc.perform(get("/api/employees/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));
	}

	@Test
	void departmentIdParamThatIsNotANumberReturns400() throws Exception {
		mockMvc.perform(get("/api/employees").param("departmentId", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'departmentId'"));
	}

	// ---- other statuses keep their meaning ----

	@Test
	void wrongMethodReturns405() throws Exception {
		mockMvc.perform(patch("/api/employees/1"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.message").value("Method PATCH is not supported for this URL"));
	}

	@Test
	void wrongContentTypeReturns415() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.TEXT_PLAIN).content("name=an"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.message").value("Content-Type must be application/json"));
	}

	@Test
	void unexpectedErrorReturns500WithoutDetails() throws Exception {
		when(employeeService.getEmployeeById(1L)).thenThrow(new RuntimeException("secret database detail"));

		mockMvc.perform(get("/api/employees/1"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").value("Unexpected error, please try again later"));
	}

}
