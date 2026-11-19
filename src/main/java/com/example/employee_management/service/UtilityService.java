package com.example.employee_management.service;

import org.springframework.stereotype.Service;

@Service
public class UtilityService {

	public static final String EMPLOYEE_CODE_PREFIX = "EMP";
	public static final int CODE_NUMBER_LENGTH = 4;

	// Number used for the next employee code. Kept in memory, so it restarts at 1 when the app restarts.
	private int nextNumber = 1;

	// so two requests at the same time must not get the same code.
	public synchronized String generateEmployeeCode() {
		int number = nextNumber;
		nextNumber++;
		return EMPLOYEE_CODE_PREFIX + padNumber(number);
	}

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

	// 7 -> "0007"; numbers with more digits are kept as they are
	private String padNumber(int number) {
		String text = String.valueOf(number);
		while (text.length() < CODE_NUMBER_LENGTH) {
			text = "0" + text;
		}
		return text;
	}

}
