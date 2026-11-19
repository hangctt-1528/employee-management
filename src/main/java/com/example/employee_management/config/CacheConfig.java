package com.example.employee_management.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

// Turns on @Cacheable / @CacheEvict. Spring Boot creates the CacheManager
// (spring.cache.type=simple: an in-memory map).
// Kept out of the main class so test slices (@WebMvcTest, @DataJpaTest) do not turn it on.
@Configuration
@EnableCaching
public class CacheConfig {
}
