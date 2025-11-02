package com.github.jwxa.scenario.dto;

/**
 * Request payload for the client-side cache invalidation scenario.
 */
public record NearCacheInvalidationRequest(String key,
                                           String initialValue,
                                           String updatedValue,
                                           Long awaitMillis) {

    private static final long DEFAULT_AWAIT = 300L;

    public NearCacheInvalidationRequest {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (initialValue == null) {
            throw new IllegalArgumentException("initialValue must not be null");
        }
        if (updatedValue == null) {
            throw new IllegalArgumentException("updatedValue must not be null");
        }
        awaitMillis = awaitMillis == null || awaitMillis < 0 ? DEFAULT_AWAIT : awaitMillis;
    }
}
