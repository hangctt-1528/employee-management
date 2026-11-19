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
