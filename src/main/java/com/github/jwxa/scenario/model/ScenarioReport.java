package com.github.jwxa.scenario.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Aggregated outcome returned by each scenario endpoint.
 */
public record ScenarioReport(String scenarioCode,
                             Instant executedAt,
                             List<ScenarioStep> steps,
                             Map<String, Object> context) {

    public ScenarioReport {
        steps = steps == null ? List.of() : List.copyOf(steps);
        context = context == null ? Map.of() : Map.copyOf(context);
        executedAt = executedAt != null ? executedAt : Instant.now();
    }
}
