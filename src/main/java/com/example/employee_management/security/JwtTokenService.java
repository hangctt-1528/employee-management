package com.example.employee_management.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

// Creates the JWT of POST /api/auth/login: header.payload.signature, signed with HS256.
@Service
public class JwtTokenService {

	public static final String ISSUER = "employee-management";
	public static final String ROLES_CLAIM = "roles";

	private final JwtEncoder jwtEncoder;
	private final long expirationSeconds;

	public JwtTokenService(JwtEncoder jwtEncoder,
			@Value("${app.security.jwt-expiration-seconds}") long expirationSeconds) {
		this.jwtEncoder = jwtEncoder;
		this.expirationSeconds = expirationSeconds;
	}

	public long getExpirationSeconds() {
		return expirationSeconds;
	}

	public String createToken(Authentication authentication) {
		List<String> roles = new ArrayList<>();
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			String name = authority.getAuthority();
			if (name.startsWith("ROLE_")) {
				roles.add(name.substring("ROLE_".length())); // "ROLE_ADMIN" -> "ADMIN"
			}
		}

		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(authentication.getName()) // the username
				.issuedAt(now)
				.expiresAt(now.plusSeconds(expirationSeconds))
				.claim(ROLES_CLAIM, roles)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}
