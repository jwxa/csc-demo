package com.github.jwxa.scenario.dto;

/**
 * Request payload for CSC string warmup / update / refresh walkthrough.
 */
public record ClientSideCachingWarmupRequest(
        String initialValue,
        String updatedValue,
        Long ttlSeconds,
        Long awaitMillis,
        Boolean refreshTtl,
        Long refreshTtlSeconds
) {

    private static final long DEFAULT_TTL_SECONDS = 30L;
    private static final long DEFAULT_AWAIT_MILLIS = 200L;

    public ClientSideCachingWarmupRequest {
        if (initialValue == null || initialValue.isBlank()) {
            throw new IllegalArgumentException("initialValue must not be blank");
        }
        updatedValue = (updatedValue == null || updatedValue.isBlank()) ? initialValue : updatedValue;
        ttlSeconds = ttlSeconds == null || ttlSeconds <= 0 ? DEFAULT_TTL_SECONDS : ttlSeconds;
        awaitMillis = awaitMillis == null || awaitMillis < 0 ? DEFAULT_AWAIT_MILLIS : awaitMillis;
        refreshTtl = refreshTtl != null && refreshTtl;
        refreshTtlSeconds = refreshTtlSeconds == null || refreshTtlSeconds <= 0 ? ttlSeconds : refreshTtlSeconds;
    }
}
