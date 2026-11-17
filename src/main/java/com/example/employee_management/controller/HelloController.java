package com.example.employee_management.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	public static final String GREETING = "Hello, Employee Management!";

	@GetMapping("/hello")
	public String hello() {
		return GREETING;
	}

}
