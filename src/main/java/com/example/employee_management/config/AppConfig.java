package com.example.employee_management.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// @EnableCaching (@Cacheable), @EnableScheduling (@Scheduled), @EnableMethodSecurity (@PreAuthorize).
// Here and not on the main class, so test slices (@WebMvcTest, @DataJpaTest) do not turn them on.
@Configuration
@EnableCaching
@EnableScheduling
@EnableMethodSecurity
public class AppConfig {

	// Custom bean (Lab 2): BCrypt, used to check passwords at login and to hash them.
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
