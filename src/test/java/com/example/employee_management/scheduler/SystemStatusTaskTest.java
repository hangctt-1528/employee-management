package com.example.employee_management.scheduler;

import static com.example.employee_management.LogTestHelper.hasLine;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

// Plain JUnit: calls the method directly instead of waiting 30 s.
// The real schedule is checked in ActuatorEndpointsTest (/actuator/scheduledtasks).
@ExtendWith(OutputCaptureExtension.class)
class SystemStatusTaskTest {

	@Test
	void logsSystemRunningAtInfo(CapturedOutput output) {
		new SystemStatusTask().logSystemRunning();

		assertTrue(hasLine(output.getOut(), "INFO", "System running"));
	}

	@Test
	void runsEvery30SecondsWithTheMessageOfTheLab() {
		assertEquals(30000, SystemStatusTask.SYSTEM_RUNNING_INTERVAL_MS);
		assertEquals("System running", SystemStatusTask.SYSTEM_RUNNING_MESSAGE);
	}

}
