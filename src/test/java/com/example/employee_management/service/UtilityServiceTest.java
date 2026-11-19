package com.example.employee_management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UtilityServiceTest {

	private UtilityService utilityService;

	@BeforeEach
	void setUp() {
		utilityService = new UtilityService();
	}

	@Test
	void firstCodeIsEmp0001() {
		assertEquals("EMP0001", utilityService.generateEmployeeCode());
	}

	@Test
	void codesIncreaseByOne() {
		utilityService.generateEmployeeCode();
		assertEquals("EMP0002", utilityService.generateEmployeeCode());
	}

	@Test
	void tenthCodeHasTwoDigits() {
		String code = "";
		for (int i = 1; i <= 10; i++) {
			code = utilityService.generateEmployeeCode();
		}
		assertEquals("EMP0010", code);
	}

	@Test
	void codeAbove9999KeepsAllDigits() {
		String code = "";
		for (int i = 1; i <= 10000; i++) {
			code = utilityService.generateEmployeeCode();
		}
		assertEquals("EMP10000", code);
	}

	@Test
	void formatNameFixesCaseAndSpaces() {
		assertEquals("Nguyen Van An", utilityService.formatName("  nguyEN   van  AN "));
	}

	@Test
	void formatNameWithOneLetter() {
		assertEquals("A", utilityService.formatName("a"));
	}

	@Test
	void formatNameKeepsVietnameseLetters() {
		assertEquals("Nguyễn Văn An", utilityService.formatName("nguyễn văn an"));
	}

	@Test
	void formatNameReturnsEmptyForBlankOrNull() {
		assertEquals("", utilityService.formatName(""));
		assertEquals("", utilityService.formatName("   "));
		assertEquals("", utilityService.formatName(null));
	}

}
