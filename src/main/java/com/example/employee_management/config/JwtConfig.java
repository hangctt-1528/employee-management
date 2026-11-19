package com.example.employee_management.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.example.employee_management.security.JwtTokenService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

// JWT beans: one secret key signs (JwtEncoder) and checks (JwtDecoder) the tokens, HS256.
@Configuration
public class JwtConfig {

	// HS256 needs at least 32 bytes
	public static final int MIN_SECRET_LENGTH = 32;

	private final String secret;

	public JwtConfig(@Value("${app.security.jwt-secret}") String secret) {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
			throw new IllegalStateException("app.security.jwt-secret (APP_JWT_SECRET) must have at least "
					+ MIN_SECRET_LENGTH + " characters");
		}
		this.secret = secret;
	}

	@Bean
	public JwtEncoder jwtEncoder() {
		return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
	}

	// HS256 by default. Checks the signature, the expiry and that "iss" is this app
	@Bean
	public JwtDecoder jwtDecoder() {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey()).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtTokenService.ISSUER));
		return decoder;
	}

	private SecretKey secretKey() {
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
