package com.github.jwxa.config;

import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class CacheMetricsConfig {

    private final MeterRegistry meterRegistry;
    private final Cache<Object, Object> caffeineCache;

    public CacheMetricsConfig(MeterRegistry meterRegistry, Cache<Object, Object> caffeineCache) {
        this.meterRegistry = meterRegistry;
        this.caffeineCache = caffeineCache;
    }

    @PostConstruct
    public void bindMetrics() {
        CaffeineCacheMetrics.monitor(meterRegistry, caffeineCache, "caffeineDemoCache");
    }
}
