package com.github.jwxa.component;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CaffeineCacheInvalidator {

    private final Cache<Object, Object> caffeineCache;

    public CaffeineCacheInvalidator(Cache<Object, Object> caffeineCache) {
        this.caffeineCache = caffeineCache;
    }

    public void invalidate(Object key) {
        log.info("开始清除caffeine缓存, key:{}", key);
        caffeineCache.invalidate(key);
    }
}