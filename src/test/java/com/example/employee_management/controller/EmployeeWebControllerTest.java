package com.example.employee_management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

import java.time.LocalDateTime;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.employee_management.config.JwtConfig;
import com.example.employee_management.config.SecurityConfig;
import com.example.employee_management.dto.DepartmentEmployeeCount;
import com.example.employee_management.dto.DepartmentResponse;
import com.example.employee_management.dto.EmployeePageResponse;
import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.dto.EmployeeStatisticsResponse;
import com.example.employee_management.exception.DepartmentNotFoundException;
import com.example.employee_management.exception.DuplicateEmailException;
import com.example.employee_management.exception.InvalidPagingException;
import com.example.employee_management.service.DepartmentService;
import com.example.employee_management.service.EmployeeService;
import com.example.employee_management.service.ReportService;

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

	@MockitoBean
	private ReportService reportService; // Lab 10: /employees/statistics

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

	// The only page (page 0, size 10) holding these employees
	private EmployeePageResponse page(List<EmployeeResponse> employees) {
		int totalPages = 1;
		if (employees.isEmpty()) {
			totalPages = 0;
		}
		return new EmployeePageResponse(employees, 0, 10, employees.size(), totalPages, true, true);
	}

	// ---- Pagination ----

	// Page 2 of 3 (23 employees, 10 per page): Prev, the numbers and Next keep the search conditions
	@Test
	void searchShowsPageLinksWithTheConditions() throws Exception {
		when(employeeService.getEmployeePage("an", 1L, 1, 10))
				.thenReturn(new EmployeePageResponse(oneEmployee(), 1, 10, 23, 3, false, false));

		mockMvc.perform(get("/employees/search").param("name", "an").param("departmentId", "1").param("page", "1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Showing <span>11</span>–<span>11</span>")))
				.andExpect(content().string(containsString("of <span>23</span>")))
				.andExpect(content().string(containsString("Page <span>2</span> of <span>3</span>")))
				.andExpect(content().string(containsString("href=\"/employees/search?name=an&amp;departmentId=1&amp;page=0&amp;size=10\"")))
				.andExpect(content().string(containsString("href=\"/employees/search?name=an&amp;departmentId=1&amp;page=2&amp;size=10\"")))
				.andExpect(content().string(containsString("<span class=\"current\">2</span>")));
	}

	// "+" and "{...}" in the name must be encoded in the page links, or page 2 would search something else
	@Test
	void pageLinksEncodeSpecialCharactersOfTheName() throws Exception {
		when(employeeService.getEmployeePage("a+b {page}", null, 0, 10))
				.thenReturn(new EmployeePageResponse(oneEmployee(), 0, 10, 15, 2, true, false));

		mockMvc.perform(get("/employees/search").param("name", "a+b {page}"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(
						"href=\"/employees/search?name=a%2Bb%20%7Bpage%7D&amp;page=1&amp;size=10\"")));
	}

	@Test
	void pageAfterTheLastOneShowsALinkBack() throws Exception {
		when(employeeService.getEmployeePage(null, null, 5, 10))
				.thenReturn(new EmployeePageResponse(new ArrayList<>(), 5, 10, 23, 3, false, true));

		mockMvc.perform(get("/employees/list").param("page", "5"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Page <span>6</span> does not exist.")))
				.andExpect(content().string(containsString("href=\"/employees/list?page=2&amp;size=10\"")));
	}

	// Two requests saving the same email at the same moment: the UNIQUE column stops one -> form error, not 500
	@Test
	void uniqueConstraintViolationIsShownOnEmailField() throws Exception {
		when(employeeService.addEmployee(any(EmployeeRequest.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate key"));

		mockMvc.perform(post("/employees/add").with(csrf())
						.param("name", "an")
						.param("email", "an@example.com")
						.param("departmentId", "1"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/add"))
				.andExpect(model().attributeHasFieldErrorCode("employee", "email", "duplicate"));
	}

	@Test
	void invalidSizeShows400Page() throws Exception {
		when(employeeService.getEmployeePage(null, null, 0, 500))
				.thenThrow(new InvalidPagingException("size must be between 1 and 100"));

		mockMvc.perform(get("/employees/list").param("size", "500"))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("error"))
				.andExpect(content().string(containsString("size must be between 1 and 100")));
	}

	// ---- /employees/list ----

	@Test
	void listShowsEmployeesFromService() throws Exception {
		when(employeeService.getEmployeePage(null, null, 0, 10)).thenReturn(page(oneEmployee()));

		mockMvc.perform(get("/employees/list"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/list"))
				.andExpect(model().attributeExists("employeePage"))
				.andExpect(content().string(containsString("Nguyen Van An")))
				.andExpect(content().string(containsString("an@example.com")));
	}

	@Test
	void listWithoutEmployeesShowsMessage() throws Exception {
		when(employeeService.getEmployeePage(null, null, 0, 10)).thenReturn(page(new ArrayList<>()));

		mockMvc.perform(get("/employees/list"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("No employees yet.")));
	}

	@Test
	void listShowsFlashMessage() throws Exception {
		when(employeeService.getEmployeePage(null, null, 0, 10)).thenReturn(page(oneEmployee()));

		mockMvc.perform(get("/employees/list").flashAttr("successMessage", "Employee Nguyen Van An was added."))
				.andExpect(content().string(containsString("Employee Nguyen Van An was added.")));
	}

	@Test
	void listEscapesHtmlInNames() throws Exception {
		List<EmployeeResponse> employees = new ArrayList<>();
		employees.add(new EmployeeResponse(1L, "<script>alert(1)</script>", "x@example.com", 1L, "IT"));
		when(employeeService.getEmployeePage(null, null, 0, 10)).thenReturn(page(employees));

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
				.andExpect(model().attributeDoesNotExist("employeePage"))
				.andExpect(content().string(not(containsString("<h2>Results</h2>"))));
		verify(employeeService, never()).getEmployeePage(any(), any(), anyInt(), anyInt());
	}

	@Test
	void searchShowsResultsAndKeepsConditions() throws Exception {
		when(employeeService.getEmployeePage("an", 1L, 0, 10)).thenReturn(page(oneEmployee()));

		mockMvc.perform(get("/employees/search").param("name", "an").param("departmentId", "1"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("searched", true))
				.andExpect(content().string(containsString("Nguyen Van An")))
				.andExpect(content().string(containsString("value=\"an\"")))
				.andExpect(content().string(containsString("value=\"1\" selected=\"selected\"")));
	}

	@Test
	void searchWithAllDepartmentsPassesNull() throws Exception {
		when(employeeService.getEmployeePage("an", null, 0, 10)).thenReturn(page(oneEmployee()));

		mockMvc.perform(get("/employees/search").param("name", "an").param("departmentId", ""))
				.andExpect(status().isOk());
		verify(employeeService).getEmployeePage("an", null, 0, 10);
	}

	@Test
	void searchWithoutResultsShowsMessage() throws Exception {
		when(employeeService.getEmployeePage("xyz", null, 0, 10)).thenReturn(page(new ArrayList<>()));

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
		when(employeeService.getEmployeePage(null, null, 0, 10)).thenThrow(new RuntimeException("secret database detail"));

		mockMvc.perform(get("/employees/list"))
				.andExpect(status().isInternalServerError())
				.andExpect(view().name("error"))
				.andExpect(content().string(containsString("Unexpected error, please try again later")))
				.andExpect(content().string(not(containsString("secret database detail"))));
	}

	// Lab 7: the details of a 500 go to the log (ERROR + stack trace), not to the page
	@Test
	void unexpectedErrorIsLoggedAsError(CapturedOutput output) throws Exception {
		when(employeeService.getEmployeePage(null, null, 0, 10)).thenThrow(new RuntimeException("secret database detail"));

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
		when(employeeService.getEmployeePage(null, null, 0, 10)).thenReturn(page(oneEmployee()));

		mockMvc.perform(get("/employees/list"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Logged in as")))
				.andExpect(content().string(containsString("<b>user</b>")))
				.andExpect(content().string(not(containsString("href=\"/employees/add\""))));
	}

	@Test
	void adminSeesTheAddLinkAndTheLogoutForm() throws Exception {
		when(employeeService.getEmployeePage(null, null, 0, 10)).thenReturn(page(oneEmployee()));

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

	// ---- Lab 10: /employees/statistics ----

	private EmployeeStatisticsResponse statistics() {
		List<DepartmentEmployeeCount> rows = new ArrayList<>();
		rows.add(new DepartmentEmployeeCount(1L, "IT", 2L));
		rows.add(new DepartmentEmployeeCount(2L, "HR", 0L));
		return new EmployeeStatisticsResponse(2, rows, LocalDateTime.of(2026, 11, 19, 10, 15, 30));
	}

	@Test
	@WithMockUser(roles = "USER")
	void statisticsPageShowsTotalAndDepartments() throws Exception {
		when(reportService.getStatistics()).thenReturn(statistics());

		mockMvc.perform(get("/employees/statistics"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/statistics"))
				.andExpect(model().attributeExists("statistics"))
				.andExpect(content().string(containsString("Total employees: <b>2</b>")))
				.andExpect(content().string(containsString("<td>IT</td>")))
				.andExpect(content().string(containsString("<td>HR</td>")))
				.andExpect(content().string(containsString("2026-11-19 10:15:30")))
				.andExpect(content().string(containsString("href=\"/employees/statistics\""))); // header link
	}

	@Test
	void statisticsPageWithoutDepartmentsShowsMessage() throws Exception {
		when(reportService.getStatistics())
				.thenReturn(new EmployeeStatisticsResponse(0, new ArrayList<>(), LocalDateTime.now()));

		mockMvc.perform(get("/employees/statistics"))
				.andExpect(content().string(containsString("No departments yet.")));
	}

	@Test
	@WithAnonymousUser
	void statisticsPageNeedsALogin() throws Exception {
		mockMvc.perform(get("/employees/statistics"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void statisticsErrorShowsErrorPage() throws Exception {
		when(reportService.getStatistics()).thenThrow(new RuntimeException("secret database detail"));

		mockMvc.perform(get("/employees/statistics"))
				.andExpect(status().isInternalServerError())
				.andExpect(view().name("error"))
				.andExpect(content().string(containsString("Unexpected error, please try again later")))
				.andExpect(content().string(not(containsString("secret database detail"))));
	}

}
