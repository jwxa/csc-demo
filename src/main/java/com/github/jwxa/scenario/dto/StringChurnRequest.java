package com.github.jwxa.scenario.dto;

public record StringChurnRequest(String prefix,
                                 Integer keyCount,
                                 Integer iterations,
                                 Integer payloadSize,
                                 Long pauseMillis) {

    private static final String DEFAULT_PREFIX = "load:key";
    private static final int DEFAULT_KEY_COUNT = 500;
    private static final int DEFAULT_ITERATIONS = 2000;
    private static final int DEFAULT_PAYLOAD_SIZE = 64;
    private static final long DEFAULT_PAUSE = 0L;

    public StringChurnRequest {
        prefix = (prefix == null || prefix.isBlank()) ? DEFAULT_PREFIX : prefix;
        keyCount = keyCount == null || keyCount <= 0 ? DEFAULT_KEY_COUNT : keyCount;
        iterations = iterations == null || iterations <= 0 ? DEFAULT_ITERATIONS : iterations;
        payloadSize = payloadSize == null || payloadSize <= 0 ? DEFAULT_PAYLOAD_SIZE : payloadSize;
        pauseMillis = pauseMillis == null || pauseMillis < 0 ? DEFAULT_PAUSE : pauseMillis;
    }
}
