package com.example.employee_management.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.employee_management.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

// One place that turns exceptions into an HTTP status + ErrorResponse JSON.
// @RestControllerAdvice = @ControllerAdvice + @ResponseBody (the return value is written as JSON).
// Spring picks the handler whose exception type is the closest match, so Exception.class is only the fallback.
@RestControllerAdvice
public class GlobalExceptionHandler {

	public static final String ACCESS_DENIED_MESSAGE = "You do not have permission to do this";

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// ---- Our own exceptions (thrown by EmployeeService) ----

	@ExceptionHandler(EmployeeNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleEmployeeNotFound(EmployeeNotFoundException ex,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(DepartmentNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleDepartmentNotFound(DepartmentNotFoundException ex,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	// ---- Bad requests detected by Spring ----

	// @Valid failed: the BindingResult inside the exception lists every invalid field
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		ErrorResponse body = new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(), "Request data is not valid", request.getRequestURI());
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			body.addFieldError(fieldError.getField(), fieldError.getDefaultMessage());
		}
		return ResponseEntity.badRequest().body(body);
	}

	// Body missing, broken JSON, or a wrong type inside the JSON (e.g. "departmentId": "abc")
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST,
				"Request body is missing or is not valid JSON (check the syntax and the data types, e.g. departmentId must be a number)",
				request);
	}

	// Wrong type in the URL: /api/employees/abc or ?departmentId=abc
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST,
				"Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'", request);
	}

	// The three handlers below keep Spring's normal status; without them the Exception handler would turn them into 500
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "Method " + ex.getMethod() + " is not supported for this URL",
				request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type must be application/json", request);
	}

	// produces = JSON: a browser asking for HTML (e.g. /employees/nope) gets Spring Boot's HTML error page instead
	@ExceptionHandler(value = NoResourceFoundException.class, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.NOT_FOUND, "No endpoint at this URL", request);
	}

	// ---- Security: from the filters (via RestAuthenticationEntryPoint / RestAccessDeniedHandler) or controllers ----

	// Wrong username or password
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password", request);
	}

	// No credentials, or a bad / expired token
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.UNAUTHORIZED,
				"Authentication required: use HTTP Basic or a Bearer token from POST /api/auth/login", request);
	}

	// Role not allowed
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		logger.warn("Access denied on {} {}", request.getMethod(), request.getRequestURI());
		return buildResponse(HttpStatus.FORBIDDEN, ACCESS_DENIED_MESSAGE, request);
	}

	// Two requests with the same email / username at the same time: the UNIQUE column stops the second one
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.CONFLICT, "The data conflicts with existing data (e.g. email already used)",
				request);
	}

	// ---- Anything else: 500 with a fixed message (never show internal details to the client) ----

	@ExceptionHandler(value = Exception.class, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		// the exception is the last argument, so the log also has the stack trace (docker compose logs app)
		logger.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error, please try again later", request);
	}

	private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message,
			HttpServletRequest request) {
		ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message,
				request.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}

}
