package com.github.jwxa.controller.scenario;

import com.github.jwxa.scenario.dto.ClientSideCachingMapEntryRequest;
import com.github.jwxa.scenario.dto.ClientSideCachingWarmupRequest;
import com.github.jwxa.scenario.dto.NearCacheInvalidationRequest;
import com.github.jwxa.scenario.dto.TtlDriftRequest;
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

    @PostMapping("/near-cache/hash-invalidation")
    public ScenarioReport simulateHashInvalidation(@RequestBody ClientSideCachingMapEntryRequest request) {
        log.info("[ScenarioController] trigger hash invalidation case, key={} field={}", request.key(), request.field());
        return scenarioService.simulateMapEntryInvalidation(request);
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
