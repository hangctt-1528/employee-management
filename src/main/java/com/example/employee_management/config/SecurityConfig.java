package com.example.employee_management.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.example.employee_management.entity.AppUser;
import com.example.employee_management.security.JwtTokenService;
import com.example.employee_management.security.RestAccessDeniedHandler;
import com.example.employee_management.security.RestAuthenticationEntryPoint;

// Two chains: /api/** and /actuator/** -> apiFilterChain (Basic or JWT, no session);
// everything else -> webFilterChain (login page, session, CSRF). In a chain the first matching rule wins.
@Configuration
public class SecurityConfig {

	private final HandlerExceptionResolver handlerExceptionResolver;

	// The resolver that runs our @ExceptionHandler methods
	public SecurityConfig(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
		this.handlerExceptionResolver = handlerExceptionResolver;
	}

	@Bean
	@Order(1)
	public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
		RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(handlerExceptionResolver);
		RestAccessDeniedHandler deniedHandler = new RestAccessDeniedHandler(handlerExceptionResolver);

		http.securityMatcher("/api/**", "/actuator/**");
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
				.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				.requestMatchers("/actuator/**").hasRole(AppUser.ADMIN)
				.requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
				.requestMatchers(HttpMethod.GET, "/api/employees/**", "/api/departments/**", "/api/reports/**")
						.hasAnyRole(AppUser.USER, AppUser.ADMIN)
				// Anything else is ADMIN only, so a forgotten rule blocks USER
				.anyRequest().hasRole(AppUser.ADMIN));
		// HTTP Basic: username and password in every request
		http.httpBasic(basic -> basic.authenticationEntryPoint(entryPoint));
		// JWT: "Authorization: Bearer <token>"; Spring checks the signature and expiry
		http.oauth2ResourceServer(oauth2 -> oauth2
				.authenticationEntryPoint(entryPoint)
				.accessDeniedHandler(deniedHandler)
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		// No session cookie, so no CSRF risk
		http.csrf(csrf -> csrf.disable());
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler));
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/login", "/register", "/css/**", "/error", "/access-denied", "/hello", "/favicon.ico")
						.permitAll()
				.requestMatchers("/employees/add").hasRole(AppUser.ADMIN)
				.requestMatchers("/employees/**").hasAnyRole(AppUser.USER, AppUser.ADMIN)
				.anyRequest().authenticated());
		// Custom GET /login page; Spring handles POST /login
		http.formLogin(form -> form
				.loginPage("/login")
				.defaultSuccessUrl("/employees/list", true)
				.permitAll());
		http.logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());
		// Logged in but not allowed -> 403 page
		http.exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"));
		// CSRF on (default): th:action adds the _csrf field
		return http.build();
	}

	// Token claim "roles": ["ADMIN"] -> ROLE_ADMIN, so hasRole(...) works
	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName(JwtTokenService.ROLES_CLAIM);
		authoritiesConverter.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}

}
