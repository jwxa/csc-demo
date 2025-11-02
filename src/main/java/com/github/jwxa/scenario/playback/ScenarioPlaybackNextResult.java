package com.github.jwxa.scenario.playback;

import com.github.jwxa.scenario.model.ScenarioStep;

import java.util.Map;

public record ScenarioPlaybackNextResult(String scenarioCode,
                                         ScenarioStep step,
                                         int remainingSteps,
                                         boolean completed,
                                         Map<String, Object> context) {

    public static ScenarioPlaybackNextResult missing() {
        return new ScenarioPlaybackNextResult(null, null, 0, true, Map.of());
    }
}
