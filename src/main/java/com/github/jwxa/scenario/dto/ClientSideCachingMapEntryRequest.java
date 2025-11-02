package com.github.jwxa.scenario.dto;

/**
 * Request payload for simulating hash (map) style keys under CSC.
 */
public record ClientSideCachingMapEntryRequest(String key,
                                               String field,
                                               String initialValue,
                                               String updatedValue,
                                               Long awaitMillis) {

    private static final long DEFAULT_WAIT = 500L;

    public ClientSideCachingMapEntryRequest {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        if (initialValue == null) {
            throw new IllegalArgumentException("initialValue must not be null");
        }
        if (updatedValue == null) {
            throw new IllegalArgumentException("updatedValue must not be null");
        }
        awaitMillis = awaitMillis == null || awaitMillis < 0 ? DEFAULT_WAIT : awaitMillis;
    }
}
