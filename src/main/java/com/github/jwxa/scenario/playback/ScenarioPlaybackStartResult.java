package com.github.jwxa.scenario.playback;

import java.util.Map;

public record ScenarioPlaybackStartResult(String token,
                                          String scenarioCode,
                                          Map<String, Object> context,
                                          int totalSteps) {
}
