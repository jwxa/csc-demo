package com.github.jwxa.scenario.dto;

/**
 * Request payload for warming up the CSC bucket scenario.
 */
public record ClientSideCachingWarmupRequest(String value, Long ttlSeconds) {

    private static final long DEFAULT_TTL = 30L;

    public ClientSideCachingWarmupRequest {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        ttlSeconds = ttlSeconds == null || ttlSeconds <= 0 ? DEFAULT_TTL : ttlSeconds;
    }
}
