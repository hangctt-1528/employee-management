package com.example.employee_management.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.employee_management.service.ReportService;

// The default cache (ConcurrentMap) never expires: clearing it every minute keeps the report
// at most 1 minute old. The first clear is 60 s after startup.
@Component
public class EmployeeCountCacheTask {

	public static final long CACHE_CLEAR_INTERVAL_MS = 60000;

	private static final Logger logger = LoggerFactory.getLogger(EmployeeCountCacheTask.class);

	// @CacheEvict empties the cache after the method returns (the scheduler calls it through the proxy)
	@Scheduled(fixedRate = CACHE_CLEAR_INTERVAL_MS, initialDelay = CACHE_CLEAR_INTERVAL_MS)
	@CacheEvict(cacheNames = ReportService.EMPLOYEE_COUNT_CACHE, allEntries = true)
	public void clearEmployeeCountCache() {
		logger.debug("Employee count cache cleared");
	}

}
