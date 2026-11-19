package com.example.employee_management.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.employee_management.dto.EmployeeRequest;
import com.example.employee_management.dto.EmployeeResponse;
import com.example.employee_management.exception.DepartmentNotFoundException;
import com.example.employee_management.exception.DuplicateEmailException;
import com.example.employee_management.service.DepartmentService;
import com.example.employee_management.service.EmployeeService;
import com.example.employee_management.service.ReportService;

import jakarta.validation.Valid;

// Errors of these pages are shown by WebExceptionHandler (HTML error page)
@Controller
@RequestMapping("/employees")
public class EmployeeWebController {

	private final EmployeeService employeeService;
	private final DepartmentService departmentService;
	private final ReportService reportService;

	public EmployeeWebController(EmployeeService employeeService, DepartmentService departmentService,
			ReportService reportService) {
		this.employeeService = employeeService;
		this.departmentService = departmentService;
		this.reportService = reportService;
	}

	// GET /employees/list?page=0&size=10 -> one page of the employee table (page is 0-based)
	@GetMapping("/list")
	public String listEmployees(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, Model model) {
		model.addAttribute("employeePage", employeeService.getEmployeePage(null, null, page, size));
		model.addAttribute("pagePath", "/employees/list"); // the page links add ?page=..&size=..
		return "employees/list";
	}

	// GET /employees/add -> empty form
	@GetMapping("/add")
	public String showAddForm(Model model) {
		model.addAttribute("employee", new EmployeeRequest());
		addDepartments(model);
		return "employees/add";
	}

	@PostMapping("/add")
	public String addEmployee(@Valid @ModelAttribute("employee") EmployeeRequest employee,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			addDepartments(model);
			return "employees/add"; // show the form again with the typed values and the errors
		}
		try {
			EmployeeResponse created = employeeService.addEmployee(employee);
			// Post/Redirect/Get: the flash message survives one redirect, so F5 does not submit the form again
			redirectAttributes.addFlashAttribute("successMessage", "Employee " + created.getName() + " was added.");
			return "redirect:/employees/list";
		} catch (DuplicateEmailException ex) {
			bindingResult.rejectValue("email", "duplicate", ex.getMessage());
		} catch (DepartmentNotFoundException ex) {
			bindingResult.rejectValue("departmentId", "notFound", ex.getMessage());
		} catch (DataIntegrityViolationException ex) {
			// the same email saved by another request at the same moment (UNIQUE column)
			bindingResult.rejectValue("email", "duplicate", "Email is already used by another employee");
		}
		addDepartments(model);
		return "employees/add";
	}

	// GET /employees/search                      -> only the search form
	// GET /employees/search?name=an&departmentId= -> form + results
	@GetMapping("/search")
	public String searchEmployees(@RequestParam(required = false) String name,
			@RequestParam(required = false) Long departmentId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, Model model) {
		boolean searched = name != null || departmentId != null; // the form was submitted
		model.addAttribute("searchName", name);
		model.addAttribute("searchDepartmentId", departmentId);
		model.addAttribute("searched", searched);
		addDepartments(model);
		if (searched) {
			model.addAttribute("employeePage", employeeService.getEmployeePage(name, departmentId, page, size));
			// The page links keep the search conditions: /employees/search?name=an&departmentId=1&page=..
			// The name goes in as a URI variable, so every special character is encoded ("a+b" -> "a%2Bb")
			UriComponentsBuilder path = UriComponentsBuilder.fromPath("/employees/search");
			if (name != null) {
				path.queryParam("name", "{name}");
			}
			if (departmentId != null) {
				path.queryParam("departmentId", departmentId);
			}
			model.addAttribute("pagePath", path.encode().buildAndExpand(name).toUriString());
		}
		return "employees/search";
	}

	// GET /employees/statistics (same data as /api/reports/statistics)
	@GetMapping("/statistics")
	public String showStatistics(Model model) {
		model.addAttribute("statistics", reportService.getStatistics());
		return "employees/statistics";
	}

	private void addDepartments(Model model) {
		model.addAttribute("departments", departmentService.getAllDepartments());
	}

}
