package com.example.employee_management.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// The JSON body of every error, built by GlobalExceptionHandler, e.g.
// {"timestamp":"...","status":404,"error":"Not Found","message":"Employee not found with id 99",
//  "path":"/api/employees/99","fieldErrors":{}}
public class ErrorResponse {

	private LocalDateTime timestamp;
	private int status;
	private String error;
	private String message;
	private String path;
	// field name -> message; filled only for validation errors, otherwise {}
	private Map<String, String> fieldErrors;

	public ErrorResponse() {
		this.timestamp = LocalDateTime.now();
		this.fieldErrors = new HashMap<>();
	}

	public ErrorResponse(int status, String error, String message, String path) {
		this();
		this.status = status;
		this.error = error;
		this.message = message;
		this.path = path;
	}

	// Keeps only the first message of each field
	public void addFieldError(String field, String message) {
		if (!fieldErrors.containsKey(field)) {
			fieldErrors.put(field, message);
		}
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Map<String, String> getFieldErrors() {
		return fieldErrors;
	}

	public void setFieldErrors(Map<String, String> fieldErrors) {
		this.fieldErrors = fieldErrors;
	}

}
