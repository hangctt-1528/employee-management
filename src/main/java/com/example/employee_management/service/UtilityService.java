package com.example.employee_management.service;

import org.springframework.stereotype.Service;

@Service
public class UtilityService {

	// "  nguyEN   van  AN " -> "Nguyen Van An"; null or blank -> ""
	public String formatName(String fullName) {
		if (fullName == null) {
			return "";
		}
		String trimmed = fullName.trim();
		if (trimmed.isEmpty()) {
			return "";
		}
		String[] parts = trimmed.split(" ");
		String result = "";
		for (String part : parts) {
			if (part.isEmpty()) {
				continue; // several spaces in a row give empty parts
			}
			if (!result.isEmpty()) {
				result = result + " ";
			}
			result = result + capitalize(part);
		}
		return result;
	}

	private String capitalize(String word) {
		return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
	}

}
