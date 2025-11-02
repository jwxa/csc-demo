package com.github.jwxa.scenario.dto;

public record NearCacheStatusRequest(String key) {
    public NearCacheStatusRequest {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
    }
}

