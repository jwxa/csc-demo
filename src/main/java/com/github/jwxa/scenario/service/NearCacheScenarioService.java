package com.github.jwxa.scenario.service;

import com.github.jwxa.scenario.dto.ClientSideCachingMapEntryRequest;
import com.github.jwxa.scenario.dto.ClientSideCachingWarmupRequest;
import com.github.jwxa.scenario.dto.EventStormRequest;
import com.github.jwxa.scenario.dto.ExpirationVerificationRequest;
import com.github.jwxa.scenario.dto.NearCacheInvalidationRequest;
import com.github.jwxa.scenario.dto.NearCacheStatusRequest;
import com.github.jwxa.scenario.dto.StringChurnRequest;
import com.github.jwxa.scenario.dto.TtlDriftRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jwxa.scenario.model.ScenarioReport;
import com.github.jwxa.scenario.model.ScenarioStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.api.ClusterNode;
import org.redisson.api.ClusterNodesGroup;
import org.redisson.api.Node;
import org.redisson.api.NodeType;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
    private final ObjectMapper objectMapper;

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

        Map<String, Object> finalObservation = new HashMap<>();
        finalObservation.put("key", request.key());
        finalObservation.put("remote", remoteAfter);
        finalObservation.put("localImmediate", localAfter);
        boolean consistent = remoteAfter == null ? localAfter == null : remoteAfter.equals(localAfter);
        finalObservation.put("consistentImmediately", consistent);
        if (!consistent) {
            long extraWait = Math.max(100L, request.awaitMillis());
            waitQuietly(extraWait);
            finalObservation.put("extraWaitMillis", extraWait);
            String localAfterDelay = scenarioClientSideCachingMap.get(request.key());
            finalObservation.put("localAfterDelay", localAfterDelay);
            boolean consistentAfterDelay = remoteAfter == null ? localAfterDelay == null : remoteAfter.equals(localAfterDelay);
            finalObservation.put("consistentAfterDelay", consistentAfterDelay);
            if (!consistentAfterDelay) {
                Map<String, String> fullReload = scenarioClientSideCachingMap.readAllMap();
                String localAfterReload = fullReload.get(request.key());
                finalObservation.put("localAfterReload", localAfterReload);
                boolean consistentAfterReload = remoteAfter == null ? localAfterReload == null : remoteAfter.equals(localAfterReload);
                finalObservation.put("consistentAfterReload", consistentAfterReload);
            }
        }
        steps.add(step("eventual-check",
                "Re-check consistency after optional extra wait to demonstrate eventual convergence",
                finalObservation));

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
                ));
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
                ));
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
        Map<String, String> baselineLocal = scenarioClientSideCachingHash.get(request.key());
        steps.add(step("warm-local",
                "Insert hash-style key into CSC map and cache locally",
                Map.of("local", baselineLocal)));

        Map<String, String> baselineRemote = coerceToMap(remoteMap.get(request.key()));
        steps.add(step("baseline-remote",
                "Verify Redis hash mirrors the local snapshot",
                Map.of("remote", baselineRemote)));

        Map<String, String> remoteMutation = new HashMap<>(baselineRemote.isEmpty() ? initial : baselineRemote);
        remoteMutation.put(request.field(), request.updatedValue());
        remoteMap.put(request.key(), remoteMutation);
        steps.add(step("remote-mutation",
                "Simulate another node updating a hash field directly in Redis",
                Map.of("mutatedField", request.field(), "updatedValue", request.updatedValue())));

        waitQuietly(request.awaitMillis());

        Map<String, String> localAfter = scenarioClientSideCachingHash.get(request.key());
        Map<String, String> remoteAfter = coerceToMap(remoteMap.get(request.key()));
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
                ));
    }


    public ScenarioReport inspectNearCacheStatus(NearCacheStatusRequest request) {
        List<ScenarioStep> steps = new ArrayList<>();
        RMap<String, String> remoteMap = redissonClient.getMap(scenarioClientSideCachingMap.getName());

        String localValue = scenarioClientSideCachingMap.get(request.key());
        String remoteValue = remoteMap.get(request.key());
        Map<String, Object> observation = new HashMap<>();
        observation.put("key", request.key());
        observation.put("local", localValue);
        observation.put("remote", remoteValue);
        observation.put("localPresent", localValue != null);
        observation.put("remotePresent", remoteValue != null);

        steps.add(step("snapshot",
                "Compare CSC local cache with Redis value for given key",
                observation));

        return new ScenarioReport(
                "near-cache-status",
                Instant.now(),
                steps,
                observation
        );
    }

    public ScenarioReport verifyExpirationPolicy(ExpirationVerificationRequest request) {
        List<ScenarioStep> steps = new ArrayList<>();

        scenarioClientSideCachingBucket.set(request.value(), request.ttlSeconds(), TimeUnit.SECONDS);
        steps.add(step("set-with-ttl",
                "Set value with requested TTL and prime local cache",
                Map.of(
                        "key", request.key(),
                        "value", request.value(),
                        "ttlSeconds", request.ttlSeconds()
                )));

        List<Map<String, Object>> probes = new ArrayList<>();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long start = System.currentTimeMillis();
        boolean expiredWithinWindow = false;
        while (System.currentTimeMillis() - start <= request.maxWaitMillis()) {
            long ttl = scenarioClientSideCachingBucket.remainTimeToLive();
            String snapshot = scenarioClientSideCachingBucket.get();
            long elapsed = System.currentTimeMillis() - start;
            Map<String, Object> probe = new HashMap<>();
            probe.put("elapsedMillis", elapsed);
            probe.put("ttlMillis", ttl);
            probe.put("value", snapshot);
            probe.put("heapUsed", memoryMXBean.getHeapMemoryUsage().getUsed());
            probes.add(probe);
            if (ttl < 0 || snapshot == null) {
                expiredWithinWindow = true;
                break;
            }
            waitQuietly(request.pollIntervalMillis());
        }

        steps.add(step("polling-window",
                "Observed TTL decay samples",
                Map.of("samples", probes)));

        scenarioClientSideCachingBucket.delete();

        steps.add(step("final-status",
                "Report whether the value expired within the observation window",
                Map.of(
                        "expired", expiredWithinWindow,
                        "observedMillis", System.currentTimeMillis() - start
                )));

        return new ScenarioReport(
                "expire-policy-verification",
                Instant.now(),
                steps,
                Map.of(
                        "bucketName", scenarioClientSideCachingBucket.getName(),
                        "maxWaitMillis", request.maxWaitMillis()
                ));
    }

    public ScenarioReport simulateEventStorm(EventStormRequest request) {
        List<ScenarioStep> steps = new ArrayList<>();
        RMap<String, String> remoteMap = redissonClient.getMap(scenarioClientSideCachingMap.getName());

        scenarioClientSideCachingMap.put(request.key(), request.initialValue());
        steps.add(step("warm-local",
                "Prime CSC map prior to stress test",
                Map.of("local", scenarioClientSideCachingMap.get(request.key()))));

        long start = System.nanoTime();
        for (int i = 0; i < request.iterations(); i++) {
            String value = request.initialValue() + "#" + (i + 1);
            remoteMap.put(request.key(), value);
            if (request.pauseMillis() > 0) {
                waitQuietly(request.pauseMillis());
            }
        }
        long durationNanos = System.nanoTime() - start;

        steps.add(step("storm-executed",
                "Executed burst of remote mutations to trigger invalidations",
                Map.of(
                        "iterations", request.iterations(),
                        "durationMillis", durationNanos / 1_000_000.0,
                        "pauseMillis", request.pauseMillis()
                )));

        String localAfter = scenarioClientSideCachingMap.get(request.key());
        String remoteAfter = remoteMap.get(request.key());
        steps.add(step("verify-after-storm",
                "Capture local vs remote value following event burst",
                Map.of("local", localAfter, "remote", remoteAfter)));

        return new ScenarioReport(
                "event-storm-simulation",
                Instant.now(),
                steps,
                Map.of(
                        "mapName", scenarioClientSideCachingMap.getName(),
                        "iterations", request.iterations()
                ));
    }

    public ScenarioReport simulateStringChurn(StringChurnRequest request) {
        List<ScenarioStep> steps = new ArrayList<>();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<String> keys = new ArrayList<>(request.keyCount());
        for (int i = 0; i < request.keyCount(); i++) {
            keys.add(request.prefix() + ":" + i);
        }

        String payload = generatePayload(request.payloadSize());
        long warmupStart = System.nanoTime();
        for (String key : keys) {
            redissonClient.getBucket(key).set(payload);
        }
        long warmupDuration = System.nanoTime() - warmupStart;
        steps.add(step("warmup",
                "Initial set of all keys with base payload",
                Map.of(
                        "keyCount", request.keyCount(),
                        "payloadSize", request.payloadSize(),
                        "warmupDurationMillis", warmupDuration / 1_000_000.0,
                        "heapUsed", memoryMXBean.getHeapMemoryUsage().getUsed()
                )));

        List<Map<String, Object>> samples = new ArrayList<>();
        long updatesStart = System.nanoTime();
        int sampleEvery = Math.max(1, request.iterations() / 10);
        for (int i = 0; i < request.iterations(); i++) {
            String key = keys.get(random.nextInt(keys.size()));
            String value = payload + "-" + i;
            redissonClient.getBucket(key).set(value);
            if (request.pauseMillis() > 0) {
                waitQuietly(request.pauseMillis());
            }
            if (i % sampleEvery == 0 || i == request.iterations() - 1) {
                Map<String, Object> probe = new HashMap<>();
                probe.put("iteration", i + 1);
                probe.put("key", key);
                probe.put("heapUsed", memoryMXBean.getHeapMemoryUsage().getUsed());
                samples.add(probe);
            }
        }
        long updatesDuration = System.nanoTime() - updatesStart;

        steps.add(step("churn-loop",
                "Performed churning updates across keys",
                Map.of(
                        "iterations", request.iterations(),
                        "durationMillis", updatesDuration / 1_000_000.0,
                        "samples", samples
                )));

        long finalHeap = memoryMXBean.getHeapMemoryUsage().getUsed();
        steps.add(step("summary",
                "Final heap usage snapshot",
                Map.of("heapUsed", finalHeap)));

        return new ScenarioReport(
                "string-churn-simulation",
                Instant.now(),
                steps,
                Map.of(
                        "keyCount", request.keyCount(),
                        "iterations", request.iterations(),
                        "payloadSize", request.payloadSize()
                ));
    }

    public ScenarioReport inspectClusterTopology() {
        List<ScenarioStep> steps = new ArrayList<>();
        ClusterNodesGroup nodesGroup = redissonClient.getClusterNodesGroup();

        List<Map<String, Object>> masters = new ArrayList<>();
        nodesGroup.getNodes(NodeType.MASTER).forEach(node -> masters.add(extractNodeInfo(node)));
        steps.add(step("masters",
                "Enumerate cluster master nodes",
                Map.of("masters", masters)));

        List<Map<String, Object>> replicas = new ArrayList<>();
        nodesGroup.getNodes(NodeType.SLAVE).forEach(node -> replicas.add(extractNodeInfo(node)));
        steps.add(step("replicas",
                "Enumerate cluster replica nodes",
                Map.of("replicas", replicas)));

        return new ScenarioReport(
                "cluster-topology-inspection",
                Instant.now(),
                steps,
                Map.of(
                        "masterCount", masters.size(),
                        "replicaCount", replicas.size()
                ));
    }

    public ScenarioReport inspectReplicaReadiness() {
        List<ScenarioStep> steps = new ArrayList<>();
        ClusterNodesGroup nodesGroup = redissonClient.getClusterNodesGroup();

        List<Map<String, Object>> replicaStatus = new ArrayList<>();
        nodesGroup.getNodes(NodeType.SLAVE).forEach(node -> replicaStatus.add(extractNodeInfo(node)));
        steps.add(step("replica-status",
                "List replica nodes and health info",
                Map.of("replicas", replicaStatus)));

        if (replicaStatus.isEmpty()) {
            steps.add(step("replica-missing",
                    "No replica nodes detected in the cluster",
                    Map.of()));
        }

        return new ScenarioReport(
                "replica-readiness",
                Instant.now(),
                steps,
                Map.of("replicaCount", replicaStatus.size())
        );
    }


    private Map<String, String> coerceToMap(Object raw) {
        Map<String, String> result = new HashMap<>();
        if (raw instanceof Map<?,?> map) {
            map.forEach((k, v) -> result.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
        } else if (raw instanceof String str) {
            try {
                Map<?,?> parsed = objectMapper.readValue(str, Map.class);
                parsed.forEach((k, v) -> result.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
            } catch (Exception e) {
                log.warn("Unable to parse hash payload string for key, returning empty map", e);
            }
        }
        return result;
    }

    private Map<String, Object> extractNodeInfo(ClusterNode node) {
        Map<String, Object> info = new HashMap<>();
        info.put("address", node.getAddr().toString());
        info.put("nodeType", node.getType().name());
        info.put("ping", node.ping());
        try {
            info.put("clusterInfo", node.clusterInfo());
        } catch (Exception ignored) {
        }
        try {
            info.put("serverInfo", node.info(Node.InfoSection.SERVER));
        } catch (Exception ignored) {
        }
        return info;
    }

    private ScenarioStep step(String code, String description, Map<String, Object> observations) {
        return new ScenarioStep(code, description, observations);
    }

    private String generatePayload(int size) {
        StringBuilder builder = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            builder.append((char) ('a' + (i % 26)));
        }
        return builder.toString();
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
