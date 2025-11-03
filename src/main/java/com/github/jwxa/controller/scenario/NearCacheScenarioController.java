package com.github.jwxa.controller.scenario;

import com.github.jwxa.scenario.dto.*;
import com.github.jwxa.scenario.model.ScenarioReport;
import com.github.jwxa.scenario.service.NearCacheScenarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stand-alone controller focusing on ClientSideCaching verification scenarios.
 */
@Slf4j
@RestController
@RequestMapping("/scenario")
@RequiredArgsConstructor
public class NearCacheScenarioController {

    private final NearCacheScenarioService scenarioService;

    @PostMapping("/near-cache/invalidation")
    public ScenarioReport simulateInvalidation(@RequestBody NearCacheInvalidationRequest request) {
        log.info("[ScenarioController] trigger near-cache invalidation, key={}", request.key());
        return scenarioService.simulateNearCacheInvalidation(request);
    }

    @PostMapping("/near-cache/ttl-drift")
    public ScenarioReport simulateTtlDrift(@RequestBody TtlDriftRequest request) {
        log.info("[ScenarioController] trigger ttl drift case, key={}", request.key());
        return scenarioService.simulateTtlDrift(request);
    }


    @PostMapping("/near-cache/status")
    public ScenarioReport inspectNearCacheStatus(@RequestBody NearCacheStatusRequest request) {
        log.info("[ScenarioController] inspect near cache status, key={}", request.key());
        return scenarioService.inspectNearCacheStatus(request);
    }

    @PostMapping("/expire-policy")
    public ScenarioReport verifyExpiration(@RequestBody ExpirationVerificationRequest request) {
        log.info("[ScenarioController] verify expiration policy, key={}", request.key());
        return scenarioService.verifyExpirationPolicy(request);
    }

    @PostMapping("/event-storm")
    public ScenarioReport simulateEventStorm(@RequestBody EventStormRequest request) {
        log.info("[ScenarioController] simulate event storm, key={}", request.key());
        return scenarioService.simulateEventStorm(request);
    }

    @PostMapping("/cluster/topology")
    public ScenarioReport inspectClusterTopology() {
        log.info("[ScenarioController] inspect cluster topology snapshot");
        return scenarioService.inspectClusterTopology();
    }

    @PostMapping("/cluster/replica-readiness")
    public ScenarioReport inspectReplicaReadiness() {
        log.info("[ScenarioController] inspect replica readiness");
        return scenarioService.inspectReplicaReadiness();
    }

    @PostMapping("/csc/warmup")
    public ScenarioReport warmupClientSideCaching(@RequestBody ClientSideCachingWarmupRequest request) {
        log.info("[ScenarioController] warmup client side cache bucket");
        return scenarioService.warmupClientSideCaching(request);
    }

    @GetMapping("/csc/state")
    public ScenarioReport inspectClientSideCachingState() {
        log.info("[ScenarioController] inspect client side cache state");
        return scenarioService.inspectClientSideCachingState();
    }
}

