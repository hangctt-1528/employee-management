package com.example.employee_management;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import com.example.employee_management.security.JwtTokenService;
import com.jayway.jsonpath.JsonPath;

// The whole app with the real accounts created by UserSeeder (admin/admin123, user/user1234),
// real BCrypt and real JWTs. No test changes data: write requests of the ADMIN use an invalid body or
// an unknown id, so they pass security and stop in the controller (400 / 404), which proves access was granted.
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SecurityRulesTest {

	private static final String EMPLOYEE_BODY = "{\"name\":\"an\",\"email\":\"an@example.com\",\"departmentId\":1}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	// POST /api/auth/login and read accessToken from the JSON answer
	private String login(String username, String password) throws Exception {
		String json = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(json, "$.accessToken");
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	// ---- HTTP Basic ----

	@Test
	void apiWithoutLoginReturns401Json() throws Exception {
		mockMvc.perform(get("/api/employees"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.path").value("/api/employees"));
	}

	@Test
	void userAndAdminCanReadWithBasic() throws Exception {
		mockMvc.perform(get("/api/employees").with(httpBasic("user", "user1234"))).andExpect(status().isOk());
		mockMvc.perform(get("/api/employees").with(httpBasic("admin", "admin123"))).andExpect(status().isOk());
		mockMvc.perform(get("/api/departments").with(httpBasic("user", "user1234"))).andExpect(status().isOk());
		mockMvc.perform(get("/api/reports/employee-count").with(httpBasic("user", "user1234")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/reports/statistics").with(httpBasic("user", "user1234")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/reports/employees-by-department").with(httpBasic("user", "user1234")))
				.andExpect(status().isOk());
	}

	@Test
	void wrongPasswordReturns401() throws Exception {
		mockMvc.perform(get("/api/employees").with(httpBasic("admin", "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	void userCannotChangeEmployees() throws Exception {
		mockMvc.perform(post("/api/employees").with(httpBasic("user", "user1234"))
						.contentType(MediaType.APPLICATION_JSON).content(EMPLOYEE_BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You do not have permission to do this"));
		mockMvc.perform(delete("/api/employees/999").with(httpBasic("user", "user1234")))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminPassesSecurityForWrites() throws Exception {
		// invalid body -> 400 from @Valid: the request reached the controller
		mockMvc.perform(post("/api/employees").with(httpBasic("admin", "admin123"))
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		// unknown id -> 404 from the service
		mockMvc.perform(delete("/api/employees/999").with(httpBasic("admin", "admin123")))
				.andExpect(status().isNotFound());
	}

	// The last API rule is "ADMIN only": an /api URL without its own rule is closed to USER
	@Test
	void apiUrlWithoutARuleIsAdminOnly() throws Exception {
		mockMvc.perform(post("/api/departments").with(httpBasic("user", "user1234"))
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/something-new").with(httpBasic("user", "user1234")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/something-new").with(httpBasic("admin", "admin123")))
				.andExpect(status().isNotFound()); // allowed, but there is no such endpoint
	}

	@Test
	void userCanCallMe() throws Exception {
		mockMvc.perform(get("/api/auth/me").with(httpBasic("user", "user1234")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("USER"));
	}

	@Test
	void usernameIsNotCaseSensitive() throws Exception {
		mockMvc.perform(get("/api/employees").with(httpBasic("Admin", "admin123"))).andExpect(status().isOk());
	}

	// ---- JWT ----

	@Test
	void loginReturnsATokenThatOpensTheApi() throws Exception {
		String token = login("admin", "admin123");

		mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("admin"))
				.andExpect(jsonPath("$.role").value("ADMIN"));
		mockMvc.perform(delete("/api/employees/999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isNotFound()); // ADMIN role read from the token
	}

	// Lab 10: statistics with a USER token, and without login
	@Test
	void statisticsWorkWithAUserTokenAndNeedALogin() throws Exception {
		String token = login("user", "user1234");

		mockMvc.perform(get("/api/reports/employees-by-department").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/reports/statistics").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/reports/statistics"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void userTokenCannotDelete() throws Exception {
		String token = login("user", "user1234");

		mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/employees/999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isForbidden());
	}

	@Test
	void loginWithWrongPasswordReturns401() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	void changedTokenIsRejected() throws Exception {
		String token = login("user", "user1234");
		// change the last character of the signature
		String last = token.substring(token.length() - 1);
		String other = "A";
		if (last.equals("A")) {
			other = "B";
		}
		String changed = token.substring(0, token.length() - 1) + other;

		mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, bearer(changed)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void expiredTokenIsRejected() throws Exception {
		Instant twoHoursAgo = Instant.now().minusSeconds(7200);
		String token = adminToken(JwtTokenService.ISSUER, twoHoursAgo, twoHoursAgo.plusSeconds(3600)); // expired 1 h ago

		mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isUnauthorized());
	}

	// Signed with the right key but made by another app ("iss" is checked)
	@Test
	void tokenFromAnotherIssuerIsRejected() throws Exception {
		String token = adminToken("another-app", Instant.now(), Instant.now().plusSeconds(3600));

		mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isUnauthorized());
	}

	// Control for the two tests above: the same kind of token with the right issuer and time works
	@Test
	void handMadeTokenWithTheRightIssuerWorks() throws Exception {
		String token = adminToken(JwtTokenService.ISSUER, Instant.now(), Instant.now().plusSeconds(3600));

		mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk());
	}

	private String adminToken(String issuer, Instant issuedAt, Instant expiresAt) {
		List<String> roles = new ArrayList<>();
		roles.add("ROLE_ADMIN");
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.subject("admin")
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim(JwtTokenService.ROLES_CLAIM, roles)
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	// ---- Web pages (login form + session) ----

	@Test
	void formLoginWithTheSeededUserWorks() throws Exception {
		mockMvc.perform(formLogin("/login").user("user").password("user1234"))
				.andExpect(authenticated().withUsername("user").withRoles("USER"))
				.andExpect(redirectedUrl("/employees/list"));
	}

	@Test
	void formLoginWithWrongPasswordGoesBackToLogin() throws Exception {
		mockMvc.perform(formLogin("/login").user("user").password("wrong-password"))
				.andExpect(unauthenticated())
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void helloIsOpenToEveryone() throws Exception {
		mockMvc.perform(get("/hello")).andExpect(status().isOk());
	}

	// Unknown URL: JSON 404 for an API client, plain 404 (Boot's HTML error page) for a browser
	@Test
	void unknownUrlIsJsonForApiAndHtmlForBrowser(CapturedOutput output) throws Exception {
		mockMvc.perform(get("/api/employees/x/y").with(httpBasic("admin", "admin123")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("No endpoint at this URL"));

		// The Accept header a real browser sends (it ends with */*, which also matches JSON)
		mockMvc.perform(get("/employees/nope").with(user("user").roles("USER"))
						.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"))
				.andExpect(status().isNotFound())
				// no JSON body: Spring sends a 404 error, which Spring Boot turns into templates/error.html
				.andExpect(content().string(""));
		assertFalse(output.getOut().contains("Failure in @ExceptionHandler"));
	}

}
