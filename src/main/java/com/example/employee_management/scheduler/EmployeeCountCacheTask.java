package com.example.employee_management.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.employee_management.service.ReportService;

// The default cache (ConcurrentMap) never expires. Clearing it every minute makes the report
// at most 1 minute old. With Caffeine (spring.cache.caffeine.spec=expireAfterWrite=60s)
// this class would not be needed.
@Component
public class EmployeeCountCacheTask {

	public static final long CACHE_CLEAR_INTERVAL_MS = 60000;

	private final ReportService reportService;

	public EmployeeCountCacheTask(ReportService reportService) {
		this.reportService = reportService;
	}

	// initialDelay: the first clear is 60 s after startup (clearing the empty cache at startup is useless)
	@Scheduled(fixedRate = CACHE_CLEAR_INTERVAL_MS, initialDelay = CACHE_CLEAR_INTERVAL_MS)
	public void clearEmployeeCountCache() {
		reportService.clearEmployeeCountCache(); // through the proxy, so @CacheEvict works
	}

}
