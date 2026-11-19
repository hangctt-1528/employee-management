package com.example.employee_management.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.example.employee_management.service.ReportService;

class EmployeeCountCacheTaskTest {

	@Test
	void clearsTheReportCache() {
		ReportService reportService = mock(ReportService.class);

		new EmployeeCountCacheTask(reportService).clearEmployeeCountCache();

		verify(reportService).clearEmployeeCountCache();
	}

	@Test
	void runsEveryMinute() {
		assertEquals(60000, EmployeeCountCacheTask.CACHE_CLEAR_INTERVAL_MS);
	}

}
