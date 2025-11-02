package com.github.jwxa.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DemoService {
    private final AtomicInteger counter = new AtomicInteger();

    @Cacheable(cacheNames = "demoCache", key = "#key")
    public String getData(String key) {
        // 模拟耗时查询
        return "value-" + key + "-" + counter.incrementAndGet();
    }
}
