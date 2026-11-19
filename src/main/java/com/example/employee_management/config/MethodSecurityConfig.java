package com.example.employee_management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

// Turns on @PreAuthorize. Separate from SecurityConfig so @WebMvcTest tests do not load it.
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
