package com.github.jwxa.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public Cache<Object, Object> caffeineNativeCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(1000)
                .recordStats()   // 打开统计功能
                .build();
    }

//    @Bean
//    public CacheManager cacheManager(Cache<Object, Object> caffeineCache,
//                                     RedissonClient redissonClient,
//                                     RedisConnectionFactory redisConnectionFactory) {
//        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
//        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
//                .expireAfterWrite(10, TimeUnit.SECONDS)
//                .maximumSize(1000)
//                .recordStats());
//
////        RedissonSpringCacheManager redissonCacheManager = new RedissonSpringCacheManager(redissonClient);
//
//        RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
//                .cacheDefaults(
//                        RedisCacheConfiguration.defaultCacheConfig()
//                                .entryTtl(Duration.ofHours(1)) // 二级缓存过期时间：1 小时
//                )
//                .build();
//        // 2. 配置二级缓存（Redis）
//            CompositeCacheManager compositeCacheManager = new CompositeCacheManager();
//        compositeCacheManager.setCacheManagers(Arrays.asList(caffeineCacheManager, redisCacheManager));
////        compositeCacheManager.setCacheManagers(Arrays.asList(caffeineCacheManager, redissonCacheManager));
//        compositeCacheManager.setFallbackToNoOpCache(true);
//
//        return compositeCacheManager;
//    }

}
