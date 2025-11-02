package com.github.jwxa.scenario.playback;

import com.github.jwxa.scenario.model.ScenarioStep;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ScenarioPlaybackSession {

    private final String scenarioCode;
    private final List<ScenarioStep> steps;
    private final Map<String, Object> context;
    private final Instant createdAt;
    private final AtomicInteger cursor = new AtomicInteger(0);

    public ScenarioPlaybackSession(String scenarioCode, List<ScenarioStep> steps, Map<String, Object> context) {
        this.scenarioCode = scenarioCode;
        this.steps = steps;
        this.context = context;
        this.createdAt = Instant.now();
    }

    public String scenarioCode() {
        return scenarioCode;
    }

    public List<ScenarioStep> steps() {
        return steps;
    }

    public Map<String, Object> context() {
        return context;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public ScenarioStep nextStep() {
        int index = cursor.getAndIncrement();
        if (index >= steps.size()) {
            return null;
        }
        return steps.get(index);
    }

    public int remaining() {
        return Math.max(steps.size() - cursor.get(), 0);
    }

    public int total() {
        return steps.size();
    }

    public void reset() {
        cursor.set(0);
    }
}
