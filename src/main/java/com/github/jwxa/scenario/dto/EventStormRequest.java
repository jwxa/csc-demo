package com.github.jwxa.scenario.dto;

public record EventStormRequest(String key,
                                String initialValue,
                                Integer iterations,
                                Long pauseMillis) {

    private static final int DEFAULT_ITERATIONS = 50;
    private static final long DEFAULT_PAUSE = 0L;

    public EventStormRequest {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (initialValue == null) {
            throw new IllegalArgumentException("initialValue must not be null");
        }
        iterations = iterations == null || iterations <= 0 ? DEFAULT_ITERATIONS : iterations;
        pauseMillis = pauseMillis == null || pauseMillis < 0 ? DEFAULT_PAUSE : pauseMillis;
    }
}
