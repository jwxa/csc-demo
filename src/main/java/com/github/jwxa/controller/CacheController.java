package com.github.jwxa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Slf4j
//@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheController {

    private final RedissonClient redissonClient;
    private final RLocalCachedMap<String, String> localCachedMap;
    private final RMapCache<String, String> rMapCache;

    // ---------- RLocalCachedMap: 本地 near-cache（注意：此处不会给 Redis 上的 key 单独设置 TTL） ----------
    @PostMapping("/local/{key}/{value}")
    public String putLocal(@PathVariable String key, @PathVariable String value) {
        log.info("[LocalCachedMap] PUT key={} value={}", key, value);
        localCachedMap.put(key, value);
        // 如果你想在 Redis 层也设置 TTL，推荐把数据放到 RMapCache 或 RBucket（下面演示 RMapCache）
        return "OK";
    }

    @GetMapping("/local/{key}")
    public String getLocal(@PathVariable String key) {
        String value = localCachedMap.get(key);
        log.info("[LocalCachedMap] GET key={} value={}", key, value);
        return value;
    }

    @DeleteMapping("/local/{key}")
    public String delLocal(@PathVariable String key) {
        log.info("[LocalCachedMap] REMOVE key={}", key);
        localCachedMap.remove(key);
        return "OK";
    }

    // ---------- RMapCache: 推荐用于需要 per-entry TTL 的场景 ----------
    @PostMapping("/rmap/{key}/{value}")
    public String putRMap(@PathVariable String key, @PathVariable String value,
                          @RequestParam(defaultValue = "5") long ttlSeconds) {
        log.info("[RMapCache] PUT key={} value={} ttl={}s", key, value, ttlSeconds);
        rMapCache.put(key, value, ttlSeconds, TimeUnit.SECONDS);
        return "OK";
    }

    @GetMapping("/rmap/{key}")
    public String getRMap(@PathVariable String key) {
        String value = rMapCache.get(key);
        log.info("[RMapCache] GET key={} value={}", key, value);
        return value;
    }

    @DeleteMapping("/rmap/{key}")
    public String delRMap(@PathVariable String key) {
        log.info("[RMapCache] REMOVE key={}", key);
        rMapCache.remove(key);
        return "OK";
    }

    // 辅助：强制在 Redis 层删除整个 map（用于触发 keyspace del 事件）
    @DeleteMapping("/redis/deleteMap/{mapName}")
    public String deleteWholeMap(@PathVariable String mapName) {
        log.info("[ADMIN] delete whole redis key (map) -> {}", mapName);
        redissonClient.getBucket(mapName).delete(); // or redissonClient.getMap(mapName).delete();
        return "OK";
    }
}
