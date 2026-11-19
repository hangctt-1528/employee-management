package com.example.employee_management.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
		"spring.jpa.hibernate.ddl-auto=create-drop" })
@ActiveProfiles("prod")
class ProdProfileConfigTest {

	@Autowired
	private Environment environment;

	@Value("${logging.level.com.example.employee_management}")
	private String appLogLevel;

	@Value("${spring.jpa.show-sql}")
	private String showSql;

	@Test
	void prodProfileIsActive() {
		String[] expected = { "prod" };
		assertArrayEquals(expected, environment.getActiveProfiles());
	}

	@Test
	void prodProfileLogsInfoWithoutSql() {
		assertEquals("INFO", appLogLevel);
		assertEquals("false", showSql);
	}

}
