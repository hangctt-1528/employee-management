package com.example.employee_management.dto;

// Result of POST /api/auth/login; send it as "Authorization: Bearer <accessToken>"
public class TokenResponse {

	private String accessToken;
	private String tokenType;
	private long expiresIn; // seconds

	public TokenResponse() {
	}

	public TokenResponse(String accessToken, String tokenType, long expiresIn) {
		this.accessToken = accessToken;
		this.tokenType = tokenType;
		this.expiresIn = expiresIn;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(long expiresIn) {
		this.expiresIn = expiresIn;
	}

}
