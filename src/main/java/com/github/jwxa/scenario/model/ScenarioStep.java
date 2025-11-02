package com.github.jwxa.scenario.model;

import java.util.Map;

/**
 * Snapshot of a single step inside a scenario run.
 */
public record ScenarioStep(String code, String description, Map<String, Object> observations) {

    public ScenarioStep {
        observations = observations == null ? Map.of() : Map.copyOf(observations);
    }
}
