package com.example.employee_management.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

// Loads the real src/main/resources/application-dev.yml.
// The properties below have the highest priority, so the test uses H2 instead of the MySQL of the dev profile.
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:dev_profile_db;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop" })
@ActiveProfiles("dev")
class DevProfileConfigTest {

	@Autowired
	private Environment environment;

	@Value("${logging.level.com.example.employee_management}")
	private String appLogLevel;

	@Value("${logging.level.org.hibernate.SQL}")
	private String sqlLogLevel;

	@Value("${spring.thymeleaf.cache}")
	private String thymeleafCache;

	@Test
	void devProfileIsActive() {
		String[] expected = { "dev" };
		assertArrayEquals(expected, environment.getActiveProfiles());
	}

	@Test
	void devProfileLogsDebugAndSql() {
		assertEquals("DEBUG", appLogLevel);
		assertEquals("DEBUG", sqlLogLevel);
	}

	@Test
	void devProfileDoesNotCacheTemplates() {
		assertEquals("false", thymeleafCache);
	}

}
