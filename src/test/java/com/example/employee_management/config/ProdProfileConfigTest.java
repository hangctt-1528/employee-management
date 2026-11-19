package com.example.employee_management.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

// Loads the real src/main/resources/application-prod.yml.
// The datasource properties below replace ${SPRING_DATASOURCE_URL}..., which have no default in prod.
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:prod_profile_db;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		// Lab 9: no default in prod either
		"app.security.admin-password=test-admin-pass",
		"app.security.user-password=test-user-pass",
		"app.security.jwt-secret=prod-profile-test-secret-0123456789abcdef" })
@ActiveProfiles("prod")
class ProdProfileConfigTest {

	@Autowired
	private Environment environment;

	@Value("${logging.level.com.example.employee_management}")
	private String appLogLevel;

	@Test
	void prodProfileIsActive() {
		String[] expected = { "prod" };
		assertArrayEquals(expected, environment.getActiveProfiles());
	}

	@Test
	void prodProfileLogsInfoWithoutSql() {
		assertEquals("INFO", appLogLevel);
		assertNull(environment.getProperty("logging.level.org.hibernate.SQL")); // SQL is not logged
	}

	// Secure cookie only when APP_COOKIE_SECURE=true (behind HTTPS); off by default so plain HTTP login works
	@Test
	void sessionCookieIsNotSecureByDefault() {
		assertEquals("false", environment.getProperty("server.servlet.session.cookie.secure"));
	}

}
