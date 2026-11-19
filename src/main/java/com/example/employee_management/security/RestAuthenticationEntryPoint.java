package com.example.employee_management.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 401 JSON for the API chain. Filters run before Spring MVC, so the error is passed
// to the HandlerExceptionResolver to reach GlobalExceptionHandler.
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final HandlerExceptionResolver resolver;

	public RestAuthenticationEntryPoint(HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) {
		resolver.resolveException(request, response, null, authException);
	}

}
