package com.example.employee_management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.employee_management.dto.LoginRequest;
import com.example.employee_management.dto.TokenResponse;
import com.example.employee_management.dto.UserResponse;
import com.example.employee_management.security.JwtTokenService;
import com.example.employee_management.service.UserService;

import jakarta.validation.Valid;

// REST login (JWT). Accounts are created on the /register page.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;
	private final UserService userService;

	public AuthController(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService,
			UserService userService) {
		this.authenticationManager = authenticationManager;
		this.jwtTokenService = jwtTokenService;
		this.userService = userService;
	}

	// POST /api/auth/login -> 200 + JWT; wrong credentials -> BadCredentialsException -> 401
	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
		String token = jwtTokenService.createToken(authentication);
		return ResponseEntity.ok(new TokenResponse(token, "Bearer", jwtTokenService.getExpirationSeconds()));
	}

	// GET /api/auth/me -> the logged-in user (to test a login)
	@GetMapping("/me")
	public ResponseEntity<UserResponse> me(Authentication authentication) {
		return ResponseEntity.ok(userService.getUser(authentication.getName()));
	}

}
