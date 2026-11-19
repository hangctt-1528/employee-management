package com.example.employee_management.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import com.example.employee_management.controller.AuthWebController;
import com.example.employee_management.controller.EmployeeWebController;

import jakarta.servlet.http.HttpServletRequest;

// Errors of the Thymeleaf pages -> templates/error.html (not JSON).
// Runs before GlobalExceptionHandler, but only for the two web controllers.
@ControllerAdvice(assignableTypes = { EmployeeWebController.class, AuthWebController.class })
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(WebExceptionHandler.class);

	// e.g. /employees/search?departmentId=abc
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ModelAndView handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		return errorPage(HttpStatus.BAD_REQUEST,
				"Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'", request.getRequestURI());
	}

	// e.g. /employees/list?size=500
	@ExceptionHandler(InvalidPagingException.class)
	public ModelAndView handleInvalidPaging(InvalidPagingException ex, HttpServletRequest request) {
		return errorPage(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
	}

	// From @PreAuthorize; without it the Exception handler below would show 500
	@ExceptionHandler(AccessDeniedException.class)
	public ModelAndView handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return errorPage(HttpStatus.FORBIDDEN, GlobalExceptionHandler.ACCESS_DENIED_MESSAGE, request.getRequestURI());
	}

	@ExceptionHandler(Exception.class)
	public ModelAndView handleUnexpected(Exception ex, HttpServletRequest request) {
		logger.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error, please try again later",
				request.getRequestURI());
	}

	// Also used by AuthWebController for the /access-denied page
	public static ModelAndView errorPage(HttpStatus status, String message, String path) {
		ModelAndView modelAndView = new ModelAndView("error");
		modelAndView.setStatus(status);
		modelAndView.addObject("status", status.value());
		modelAndView.addObject("error", status.getReasonPhrase());
		modelAndView.addObject("message", message);
		modelAndView.addObject("path", path);
		return modelAndView;
	}

}
