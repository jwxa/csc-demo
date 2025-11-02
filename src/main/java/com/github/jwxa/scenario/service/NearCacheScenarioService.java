package com.github.jwxa.scenario.service;

import com.github.jwxa.scenario.dto.ClientSideCachingMapEntryRequest;
import com.github.jwxa.scenario.dto.ClientSideCachingWarmupRequest;
import com.github.jwxa.scenario.dto.NearCacheInvalidationRequest;
import com.github.jwxa.scenario.dto.TtlDriftRequest;
import com.github.jwxa.scenario.model.ScenarioReport;
import com.github.jwxa.scenario.model.ScenarioStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Scenario workflows built on top of Redisson ClientSideCaching primitives.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NearCacheScenarioService {

    private final RedissonClient redissonClient;
    private final RMap<String, String> scenarioClientSideCachingMap;
    private final RBucket<String> scenarioClientSideCachingBucket;
    private final RMap<String, Map<String, String>> scenarioClientSideCachingHash;

    public ScenarioReport simulateNearCacheInvalidation(NearCacheInvalidationRequest request) {
        List<ScenarioStep> steps = new ArrayList<>();
        RMap<String, String> remoteMap = redissonClient.getMap(scenarioClientSideCachingMap.getName());

        scenarioClientSideCachingMap.put(request.key(), request.initialValue());
        steps.add(step("warm-local",
                "Prime CSC map with initial value and build local cache",
                Map.of("local", scenarioClientSideCachingMap.get(request.key()))));

        steps.add(step("baseline-remote",
                "Verify Redis value matches local cache snapshot",
                Map.of("remote", remoteMap.get(request.key()))));

        remoteMap.put(request.key(), request.updatedValue());
        steps.add(step("publish-update",
                "Simulate another instance updating Redis to trigger CSC invalidation",
                Map.of("updatedValue", request.updatedValue())));

        waitQuietly(request.awaitMillis());

        String localAfter = scenarioClientSideCachingMap.get(request.key());
        String remoteAfter = remoteMap.get(request.key());
        steps.add(step("verify-after-window",
                "After the wait window compare local and remote values",
                Map.of("local", localAfter, "remote", remoteAfter)));

        log.info("[Scenario] invalidation -> key={} initial={} updated={} localAfter={}",
                request.key(), request.initialValue(), request.updatedValue(), localAfter);

        return new ScenarioReport(
                "near-cache-csc-invalidation",
                Instant.now(),
                steps,
                Map.of(
                        "mapName", scenarioClientSideCachingMap.getName(),
                        "awaitMillis", request.awaitMillis(),
                        "updatedValue", request.updatedValue()
                ));
    }

    public ScenarioReport simulateTtlDrift(TtlDriftRequest request) {
        List<ScenarioStep> steps = new ArrayList<>();

        scenarioClientSideCachingMap.put(request.key(), request.value());
        steps.add(step("warm-local",
                "Insert value through CSC map (local TTL controlled by CSC options)",
                Map.of("local", scenarioClientSideCachingMap.get(request.key()))));

        RMap<String, String> remoteMap = redissonClient.getMap(scenarioClientSideCachingMap.getName());
        remoteMap.expire(request.redisTtlSeconds(), TimeUnit.SECONDS);
        steps.add(step("apply-redis-ttl",
                "Set Redis map TTL to simulate faster expiration on the server side",
                Map.of("redisTTLSeconds", request.redisTtlSeconds())));

        waitQuietly(request.waitMillis());

        String localAfter = scenarioClientSideCachingMap.get(request.key());
        String remoteAfter = remoteMap.get(request.key());
        long remoteTtl = remoteMap.remainTimeToLive();
        steps.add(step("compare-after-wait",
                "Compare local cache with remote state after the wait window",
                Map.of("local", localAfter, "remote", remoteAfter, "remainTimeToLiveMillis", remoteTtl)));

        return new ScenarioReport(
                "ttl-drift-between-local-and-redis",
                Instant.now(),
                steps,
                Map.of(
                        "nearCacheName", scenarioClientSideCachingMap.getName(),
                        "waitMillis", request.waitMillis()
                )
        );
    }

    public ScenarioReport warmupClientSideCaching(ClientSideCachingWarmupRequest request) {
        scenarioClientSideCachingBucket.set(request.value(), request.ttlSeconds(), TimeUnit.SECONDS);
        Map<String, Object> observation = new HashMap<>();
        observation.put("localValue", scenarioClientSideCachingBucket.get());
        observation.put("remainingTtlMillis", scenarioClientSideCachingBucket.remainTimeToLive());

        ScenarioStep step = step("warmup-csc",
                "Warm up CSC bucket with TTL for upcoming failover or network drills",
                observation);

        return new ScenarioReport(
                "client-side-cache-warmup",
                Instant.now(),
                List.of(step),
                Map.of(
                        "bucketName", scenarioClientSideCachingBucket.getName(),
                        "ttlSeconds", request.ttlSeconds()
                )
        );
    }

    public ScenarioReport inspectClientSideCachingState() {
        Map<String, Object> context = new HashMap<>();
        context.put("bucketName", scenarioClientSideCachingBucket.getName());
        context.put("localValue", scenarioClientSideCachingBucket.get());
        context.put("remainingTtlMillis", scenarioClientSideCachingBucket.remainTimeToLive());

        return new ScenarioReport(
                "client-side-cache-state",
                Instant.now(),
                List.of(step("inspect", "Inspect current CSC bucket snapshot", context)),
                context
        );
    }

    public ScenarioReport simulateMapEntryInvalidation(ClientSideCachingMapEntryRequest request) {
        List<ScenarioStep> steps = new ArrayList<>();
        RMap<String, Map<String, String>> remoteMap = redissonClient.getMap(scenarioClientSideCachingHash.getName());

        Map<String, String> initial = new HashMap<>();
        initial.put(request.field(), request.initialValue());
        scenarioClientSideCachingHash.put(request.key(), initial);
        steps.add(step("warm-local",
                "Insert hash-style key into CSC map and cache locally",
                Map.of("local", scenarioClientSideCachingHash.get(request.key()))));

        steps.add(step("baseline-remote",
                "Verify Redis hash mirrors the local snapshot",
                Map.of("remote", remoteMap.get(request.key()))));

        Map<String, String> remoteMutation = new HashMap<>(remoteMap.getOrDefault(request.key(), Map.of()));
        remoteMutation.put(request.field(), request.updatedValue());
        remoteMap.put(request.key(), remoteMutation);
        steps.add(step("remote-mutation",
                "Simulate another node updating a hash field directly in Redis",
                Map.of("mutatedField", request.field(), "updatedValue", request.updatedValue())));

        waitQuietly(request.awaitMillis());

        Map<String, String> localAfter = scenarioClientSideCachingHash.get(request.key());
        Map<String, String> remoteAfter = remoteMap.get(request.key());
        steps.add(step("verify-after-window",
                "After wait window compare local and remote hash values",
                Map.of("local", localAfter, "remote", remoteAfter, "observedField", request.field())));

        return new ScenarioReport(
                "near-cache-csc-hash-invalidation",
                Instant.now(),
                steps,
                Map.of(
                        "hashName", scenarioClientSideCachingHash.getName(),
                        "field", request.field(),
                        "awaitMillis", request.awaitMillis()
                )
        );
    }

    private ScenarioStep step(String code, String description, Map<String, Object> observations) {
        return new ScenarioStep(code, description, observations);
    }

    private void waitQuietly(long millis) {
        try {
            if (millis > 0) {
                Thread.sleep(millis);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting inside scenario simulation", ex);
        }
    }
}
