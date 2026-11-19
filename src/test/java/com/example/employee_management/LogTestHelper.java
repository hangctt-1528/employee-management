package com.example.employee_management;

// Helper for the log tests (Lab 7).
// It checks the level and the message on the SAME log line, so an INFO line written by another class
// (e.g. Hibernate) cannot make a test pass when our line has the wrong level.
public final class LogTestHelper {

	private LogTestHelper() {
	}

	// true if one line of the captured output contains both the level (e.g. "INFO") and the message
	public static boolean hasLine(String log, String level, String message) {
		String[] lines = log.split("\n");
		for (String line : lines) {
			if (line.contains(message) && line.contains(level)) {
				return true;
			}
		}
		return false;
	}

}
