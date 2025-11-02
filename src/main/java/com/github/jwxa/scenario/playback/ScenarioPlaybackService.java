package com.github.jwxa.scenario.playback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jwxa.scenario.dto.ClientSideCachingMapEntryRequest;
import com.github.jwxa.scenario.dto.ClientSideCachingWarmupRequest;
import com.github.jwxa.scenario.dto.EventStormRequest;
import com.github.jwxa.scenario.dto.ExpirationVerificationRequest;
import com.github.jwxa.scenario.dto.NearCacheInvalidationRequest;
import com.github.jwxa.scenario.dto.TtlDriftRequest;
import com.github.jwxa.scenario.model.ScenarioReport;
import com.github.jwxa.scenario.model.ScenarioStep;
import com.github.jwxa.scenario.service.NearCacheScenarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ScenarioPlaybackService {

    private final NearCacheScenarioService scenarioService;
    private final ObjectMapper objectMapper;
    private final Map<String, ScenarioPlaybackSession> sessions = new ConcurrentHashMap<>();

    public ScenarioPlaybackStartResult start(String scenario, Map<String, Object> parameters) {
        ScenarioReport report = executeScenario(scenario, parameters);
        List<ScenarioStep> steps = report.steps();
        String token = UUID.randomUUID().toString();
        ScenarioPlaybackSession session = new ScenarioPlaybackSession(report.scenarioCode(), steps, report.context());
        sessions.put(token, session);

        return new ScenarioPlaybackStartResult(
                token,
                report.scenarioCode(),
                report.context(),
                session.total()
        );
    }

    public ScenarioPlaybackNextResult next(String token) {
        ScenarioPlaybackSession session = sessions.get(token);
        if (session == null) {
            return ScenarioPlaybackNextResult.missing();
        }
        ScenarioStep step = session.nextStep();
        boolean completed = step == null;
        if (completed) {
            sessions.remove(token);
        }
        return new ScenarioPlaybackNextResult(
                session.scenarioCode(),
                step,
                session.remaining(),
                completed,
                session.context()
        );
    }

    public void reset(String token) {
        sessions.remove(token);
    }

    private ScenarioReport executeScenario(String scenario, Map<String, Object> parameters) {
        return switch (scenario) {
            case "invalidation" -> scenarioService.simulateNearCacheInvalidation(
                    convert(parameters, NearCacheInvalidationRequest.class));
            case "ttl-drift" -> scenarioService.simulateTtlDrift(
                    convert(parameters, TtlDriftRequest.class));
            case "hash-invalidation" -> scenarioService.simulateMapEntryInvalidation(
                    convert(parameters, ClientSideCachingMapEntryRequest.class));
            case "csc-state" -> scenarioService.inspectClientSideCachingState();
            case "csc-warmup" -> scenarioService.warmupClientSideCaching(
                    convert(parameters, ClientSideCachingWarmupRequest.class));
            case "expire-policy" -> scenarioService.verifyExpirationPolicy(
                    convert(parameters, ExpirationVerificationRequest.class));
            case "event-storm" -> scenarioService.simulateEventStorm(
                    convert(parameters, EventStormRequest.class));
            case "cluster-topology" -> scenarioService.inspectClusterTopology();
            case "replica-readiness" -> scenarioService.inspectReplicaReadiness();
            default -> throw new IllegalArgumentException("Unsupported scenario: " + scenario);
        };
    }

    private <T> T convert(Map<String, Object> parameters, Class<T> target) {
        return objectMapper.convertValue(parameters, target);
    }
}


