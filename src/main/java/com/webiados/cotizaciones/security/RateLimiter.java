package com.webiados.cotizaciones.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.webiados.cotizaciones.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {

    private final Cache<String, AtomicInteger> attempts;
    private final int maxAttempts;

    public RateLimiter(AppProperties props) {
        this.maxAttempts = props.ratelimit().unlockMaxAttempts();
        int windowMinutes = props.ratelimit().unlockWindowMinutes();
        this.attempts = Caffeine.newBuilder()
                .expireAfterWrite(windowMinutes, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    /** @return true si se debe bloquear la petición (superó el límite) */
    public boolean isBlocked(String key) {
        AtomicInteger counter = attempts.get(key, k -> new AtomicInteger(0));
        return counter.get() >= maxAttempts;
    }

    public void record(String key) {
        AtomicInteger counter = attempts.get(key, k -> new AtomicInteger(0));
        counter.incrementAndGet();
    }

    public void reset(String key) {
        attempts.invalidate(key);
    }
}
