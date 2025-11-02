package com.github.jwxa.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RClientSideCaching;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.TrackingListener;
import org.redisson.api.options.ClientSideCachingOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

/**
 * Provides ClientSideCaching-based structures for near cache experiments, avoiding Redisson LocalCachedMap.
 */
@Configuration
@Slf4j
public class ScenarioNearCacheConfig {

    private static final String CSC_MAP_NAME = "scenario:csc-map";
    private static final String CSC_BUCKET_NAME = "scenario:csc-bucket";
    private static final String CSC_HASH_NAME = "scenario:csc-map-hash";

    @Bean
    public RMap<String, String> scenarioClientSideCachingMap(RedissonClient client) {
        ClientSideCachingOptions mapOptions = ClientSideCachingOptions.defaults()
                .size(1024)
                .timeToLive(Duration.ofMinutes(5))
                .maxIdle(Duration.ofMinutes(2));
        RClientSideCaching csc = client.getClientSideCaching(mapOptions);
        return csc.getMap(CSC_MAP_NAME);
    }

    @Bean
    public RBucket<String> scenarioClientSideCachingBucket(RedissonClient client) {
        ClientSideCachingOptions bucketOptions = ClientSideCachingOptions.defaults()
                .size(512)
                .timeToLive(Duration.ofMinutes(2))
                .maxIdle(Duration.ofMinutes(5));
        RClientSideCaching csc = client.getClientSideCaching(bucketOptions);
        RBucket<String> bucket = csc.getBucket(CSC_BUCKET_NAME);
        bucket.addListener((TrackingListener) name ->
                log.debug("[ScenarioNearCacheConfig] CSC bucket tracking event -> {}", name));
        return bucket;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public RMap<String, Map<String, String>> scenarioClientSideCachingHash(RedissonClient client) {
        ClientSideCachingOptions hashOptions = ClientSideCachingOptions.defaults()
                .size(512)
                .timeToLive(Duration.ofMinutes(3))
                .maxIdle(Duration.ofMinutes(5));
        RClientSideCaching csc = client.getClientSideCaching(hashOptions);
        RMap<String, Map<String, String>> hashMap = csc.getMap(CSC_HASH_NAME);
        hashMap.addListener((TrackingListener) name ->
                log.debug("[ScenarioNearCacheConfig] CSC hash tracking event -> {}", name));
        return hashMap;
    }
}
