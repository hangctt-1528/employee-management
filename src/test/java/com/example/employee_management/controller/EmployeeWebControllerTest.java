package com.example.employee_management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.employee_management.config.JwtConfig;
import com.example.employee_management.config.SecurityConfig;
import com.example.employee_management.dto.DepartmentResponse;
import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.exception.DepartmentNotFoundException;
import com.example.employee_management.exception.DuplicateEmailException;
import com.example.employee_management.service.DepartmentService;
import com.example.employee_management.service.EmployeeService;

// @WebMvcTest also starts Thymeleaf, so the tests check the view name, the model and the rendered HTML.
@WebMvcTest(EmployeeWebController.class)
@Import({ SecurityConfig.class, JwtConfig.class }) // Lab 9: our security rules, not the default ones
@WithMockUser(roles = "ADMIN") // every request runs as a logged-in ADMIN unless a test says otherwise
@ExtendWith(OutputCaptureExtension.class) // lets a test method take CapturedOutput (what was printed to the console)
class EmployeeWebControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmployeeService employeeService;

	@MockitoBean
	private DepartmentService departmentService;

	@BeforeEach
	void setUp() {
		List<DepartmentResponse> departments = new ArrayList<>();
		departments.add(new DepartmentResponse(1L, "IT"));
		departments.add(new DepartmentResponse(2L, "HR"));
		when(departmentService.getAllDepartments()).thenReturn(departments);
	}

	private List<EmployeeResponse> oneEmployee() {
		List<EmployeeResponse> employees = new ArrayList<>();
		employees.add(new EmployeeResponse(1L, "Nguyen Van An", "an@example.com", 1L, "IT"));
		return employees;
	}

	// ---- /employees/list ----

	@Test
	void listShowsEmployeesFromService() throws Exception {
		when(employeeService.getAllEmployees()).thenReturn(oneEmployee());

		mockMvc.perform(get("/employees/list"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/list"))
				.andExpect(model().attributeExists("employees"))
				.andExpect(content().string(containsString("Nguyen Van An")))
				.andExpect(content().string(containsString("an@example.com")));
	}

	@Test
	void listWithoutEmployeesShowsMessage() throws Exception {
		when(employeeService.getAllEmployees()).thenReturn(new ArrayList<>());

		mockMvc.perform(get("/employees/list"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("No employees yet.")));
	}

	@Test
	void listShowsFlashMessage() throws Exception {
		when(employeeService.getAllEmployees()).thenReturn(oneEmployee());

		mockMvc.perform(get("/employees/list").flashAttr("successMessage", "Employee Nguyen Van An was added."))
				.andExpect(content().string(containsString("Employee Nguyen Van An was added.")));
	}

	@Test
	void listEscapesHtmlInNames() throws Exception {
		List<EmployeeResponse> employees = new ArrayList<>();
		employees.add(new EmployeeResponse(1L, "<script>alert(1)</script>", "x@example.com", 1L, "IT"));
		when(employeeService.getAllEmployees()).thenReturn(employees);

		mockMvc.perform(get("/employees/list"))
				.andExpect(content().string(containsString("&lt;script&gt;")))
				.andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
	}

	// ---- GET /employees/add ----

	@Test
	void addFormIsEmptyAndListsDepartments() throws Exception {
		mockMvc.perform(get("/employees/add"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/add"))
				.andExpect(model().attributeExists("employee", "departments"))
				.andExpect(content().string(containsString("-- Select a department --")))
				.andExpect(content().string(containsString(">HR</option>")));
	}

	// ---- POST /employees/add ----

	@Test
	void validFormRedirectsToListWithFlashMessage() throws Exception {
		when(employeeService.addEmployee(any(EmployeeRequest.class)))
				.thenReturn(new EmployeeResponse(1L, "Nguyen Van An", "an@example.com", 1L, "IT"));

		mockMvc.perform(post("/employees/add").with(csrf())
						.param("name", "nguyen van an")
						.param("email", "an@example.com")
						.param("departmentId", "1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/employees/list"))
				.andExpect(flash().attribute("successMessage", "Employee Nguyen Van An was added."));
		verify(employeeService).addEmployee(any(EmployeeRequest.class));
	}

	@Test
	void blankNameShowsFormAgainWithError() throws Exception {
		mockMvc.perform(post("/employees/add").with(csrf())
						.param("name", "  ")
						.param("email", "an@example.com")
						.param("departmentId", "1"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/add"))
				.andExpect(model().attributeHasFieldErrorCode("employee", "name", "NotBlank"))
				.andExpect(content().string(containsString("Name must not be blank")))
				.andExpect(content().string(containsString("value=\"an@example.com\""))); // typed value is kept
		verify(employeeService, never()).addEmployee(any(EmployeeRequest.class));
	}

	@Test
	void invalidEmailShowsError() throws Exception {
		mockMvc.perform(post("/employees/add").with(csrf())
						.param("name", "an")
						.param("email", "not-an-email")
						.param("departmentId", "1"))
				.andExpect(view().name("employees/add"))
				.andExpect(model().attributeHasFieldErrorCode("employee", "email", "Email"))
				.andExpect(content().string(containsString("Email is not valid")));
	}

	@Test
	void missingDepartmentShowsError() throws Exception {
		mockMvc.perform(post("/employees/add").with(csrf())
						.param("name", "an")
						.param("email", "an@example.com")
						.param("departmentId", ""))
				.andExpect(view().name("employees/add"))
				.andExpect(model().attributeHasFieldErrorCode("employee", "departmentId", "NotNull"))
				.andExpect(content().string(containsString("Department is required")));
	}

	@Test
	void selectedDepartmentIsKeptAfterError() throws Exception {
		mockMvc.perform(post("/employees/add").with(csrf())
						.param("name", "")
						.param("email", "an@example.com")
						.param("departmentId", "2"))
				.andExpect(content().string(containsString("value=\"2\" selected=\"selected\"")));
	}

	@Test
	void duplicateEmailIsShownOnEmailField() throws Exception {
		when(employeeService.addEmployee(any(EmployeeRequest.class)))
				.thenThrow(new DuplicateEmailException("an@example.com"));

		mockMvc.perform(post("/employees/add").with(csrf())
						.param("name", "an")
						.param("email", "an@example.com")
						.param("departmentId", "1"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/add"))
				.andExpect(model().attributeHasFieldErrorCode("employee", "email", "duplicate"))
				.andExpect(content().string(containsString("Email an@example.com is already used by another employee")));
	}

	@Test
	void unknownDepartmentIsShownOnDepartmentField() throws Exception {
		when(employeeService.addEmployee(any(EmployeeRequest.class)))
				.thenThrow(new DepartmentNotFoundException(9L));

		mockMvc.perform(post("/employees/add").with(csrf())
						.param("name", "an")
						.param("email", "an@example.com")
						.param("departmentId", "9"))
				.andExpect(view().name("employees/add"))
				.andExpect(model().attributeHasFieldErrorCode("employee", "departmentId", "notFound"));
	}

	// ---- /employees/search ----

	@Test
	void searchPageFirstVisitShowsOnlyTheForm() throws Exception {
		mockMvc.perform(get("/employees/search"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/search"))
				.andExpect(model().attribute("searched", false))
				.andExpect(model().attributeDoesNotExist("employees"))
				.andExpect(content().string(not(containsString("<h2>Results</h2>"))));
		verify(employeeService, never()).searchEmployees(any(), any());
	}

	@Test
	void searchShowsResultsAndKeepsConditions() throws Exception {
		when(employeeService.searchEmployees("an", 1L)).thenReturn(oneEmployee());

		mockMvc.perform(get("/employees/search").param("name", "an").param("departmentId", "1"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("searched", true))
				.andExpect(content().string(containsString("Nguyen Van An")))
				.andExpect(content().string(containsString("value=\"an\"")))
				.andExpect(content().string(containsString("value=\"1\" selected=\"selected\"")));
	}

	@Test
	void searchWithAllDepartmentsPassesNull() throws Exception {
		when(employeeService.searchEmployees("an", null)).thenReturn(oneEmployee());

		mockMvc.perform(get("/employees/search").param("name", "an").param("departmentId", ""))
				.andExpect(status().isOk());
		verify(employeeService).searchEmployees("an", null);
	}

	@Test
	void searchWithoutResultsShowsMessage() throws Exception {
		when(employeeService.searchEmployees("xyz", null)).thenReturn(new ArrayList<>());

		mockMvc.perform(get("/employees/search").param("name", "xyz"))
				.andExpect(content().string(containsString("No employees found.")));
	}

	// ---- errors are HTML pages, not JSON ----

	@Test
	void departmentIdThatIsNotANumberShowsErrorPage() throws Exception {
		mockMvc.perform(get("/employees/search").param("departmentId", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("error"))
				.andExpect(content().string(containsString("Invalid value &#39;abc&#39; for parameter &#39;departmentId&#39;")));
	}

	@Test
	void unexpectedErrorShowsErrorPage() throws Exception {
		when(employeeService.getAllEmployees()).thenThrow(new RuntimeException("secret database detail"));

		mockMvc.perform(get("/employees/list"))
				.andExpect(status().isInternalServerError())
				.andExpect(view().name("error"))
				.andExpect(content().string(containsString("Unexpected error, please try again later")))
				.andExpect(content().string(not(containsString("secret database detail"))));
	}

	// Lab 7: the details of a 500 go to the log (ERROR + stack trace), not to the page
	@Test
	void unexpectedErrorIsLoggedAsError(CapturedOutput output) throws Exception {
		when(employeeService.getAllEmployees()).thenThrow(new RuntimeException("secret database detail"));

		mockMvc.perform(get("/employees/list"))
				.andExpect(status().isInternalServerError());

		String log = output.getOut();
		assertTrue(log.contains("ERROR"));
		assertTrue(log.contains("Unexpected error on GET /employees/list"));
		assertTrue(log.contains("secret database detail"));
	}

	// ---- Lab 9: roles ----

	@Test
	@WithMockUser(username = "user", roles = "USER")
	void userSeesTheListWithoutTheAddLink() throws Exception {
		when(employeeService.getAllEmployees()).thenReturn(oneEmployee());

		mockMvc.perform(get("/employees/list"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Logged in as")))
				.andExpect(content().string(containsString("<b>user</b>")))
				.andExpect(content().string(not(containsString("href=\"/employees/add\""))));
	}

	@Test
	void adminSeesTheAddLinkAndTheLogoutForm() throws Exception {
		when(employeeService.getAllEmployees()).thenReturn(oneEmployee());

		mockMvc.perform(get("/employees/list"))
				.andExpect(content().string(containsString("href=\"/employees/add\"")))
				.andExpect(content().string(containsString("action=\"/logout\"")));
	}

	@Test
	@WithMockUser(roles = "USER")
	void userCannotOpenTheAddPage() throws Exception {
		mockMvc.perform(get("/employees/add"))
				.andExpect(status().isForbidden())
				.andExpect(forwardedUrl("/access-denied"));
	}

	@Test
	@WithMockUser(roles = "USER")
	void userCannotPostTheAddForm() throws Exception {
		mockMvc.perform(post("/employees/add").with(csrf())
						.param("name", "an")
						.param("email", "an@example.com")
						.param("departmentId", "1"))
				.andExpect(status().isForbidden());
		verify(employeeService, never()).addEmployee(any(EmployeeRequest.class));
	}

	@Test
	@WithAnonymousUser
	void visitorIsSentToTheLoginPage() throws Exception {
		mockMvc.perform(get("/employees/list"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	// @PreAuthorize in the service throws AccessDeniedException: the page must say 403, not 500
	@Test
	void accessDeniedFromTheServiceShows403Page() throws Exception {
		when(employeeService.addEmployee(any(EmployeeRequest.class))).thenThrow(new AccessDeniedException("denied"));

		mockMvc.perform(post("/employees/add").with(csrf())
						.param("name", "an")
						.param("email", "an@example.com")
						.param("departmentId", "1"))
				.andExpect(status().isForbidden())
				.andExpect(view().name("error"))
				.andExpect(content().string(containsString("You do not have permission to do this")));
	}

}
