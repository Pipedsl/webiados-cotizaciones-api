package com.webiados.cotizaciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Admin admin,
        Quote quote,
        Mail mail,
        Cors cors,
        Ratelimit ratelimit
) {
    public record Admin(String bootstrapEmail, String bootstrapPassword) {
    }

    public record Quote(int validityDays) {
    }

    public record Mail(String from, String notifyTo) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Ratelimit(int unlockMaxAttempts, int unlockWindowMinutes) {
    }
}
