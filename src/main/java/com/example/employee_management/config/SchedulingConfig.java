package com.example.employee_management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Turns on @Scheduled methods (SystemStatusTask, EmployeeCountCacheTask).
// Same reason as CacheConfig for not using the main class.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
