package com.github.jwxa.scenario.dto;

public record ExpirationVerificationRequest(String key,
                                            String value,
                                            Long ttlSeconds,
                                            Long pollIntervalMillis,
                                            Long maxWaitMillis) {

    private static final long DEFAULT_TTL_SECONDS = 5L;
    private static final long DEFAULT_POLL_INTERVAL = 500L;
    private static final long DEFAULT_MAX_WAIT = 10000L;

    public ExpirationVerificationRequest {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        ttlSeconds = ttlSeconds == null || ttlSeconds <= 0 ? DEFAULT_TTL_SECONDS : ttlSeconds;
        pollIntervalMillis = pollIntervalMillis == null || pollIntervalMillis <= 0 ? DEFAULT_POLL_INTERVAL : pollIntervalMillis;
        maxWaitMillis = maxWaitMillis == null || maxWaitMillis <= 0 ? DEFAULT_MAX_WAIT : maxWaitMillis;
    }
}
