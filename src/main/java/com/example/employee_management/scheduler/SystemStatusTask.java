package com.example.employee_management.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Runs every 30 s on the "scheduling-1" thread (needs @EnableScheduling, see SchedulingConfig).
// fixedRate: the first run is right after startup.
@Component
public class SystemStatusTask {

	public static final long SYSTEM_RUNNING_INTERVAL_MS = 30000;
	public static final String SYSTEM_RUNNING_MESSAGE = "System running";

	private static final Logger logger = LoggerFactory.getLogger(SystemStatusTask.class);

	@Scheduled(fixedRate = SYSTEM_RUNNING_INTERVAL_MS)
	public void logSystemRunning() {
		logger.info(SYSTEM_RUNNING_MESSAGE);
	}

}
