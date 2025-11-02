package com.github.jwxa.scenario.dto;

/**
 * Request payload for the TTL drift simulation.
 */
public record TtlDriftRequest(String key,
                              String value,
                              Long redisTtlSeconds,
                              Long waitMillis) {

    private static final long DEFAULT_REDIS_TTL = 5L;
    private static final long DEFAULT_WAIT = 6000L;

    public TtlDriftRequest {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        redisTtlSeconds = redisTtlSeconds == null || redisTtlSeconds <= 0 ? DEFAULT_REDIS_TTL : redisTtlSeconds;
        waitMillis = waitMillis == null || waitMillis <= 0 ? DEFAULT_WAIT : waitMillis;
    }
}
