package com.gearshiftgaming.se_mod_manager.backend.domain;

public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
