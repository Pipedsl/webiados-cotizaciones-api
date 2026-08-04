package com.webiados.cotizaciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Admin admin,
        Quote quote,
        Mail mail,
        Cors cors,
        Ratelimit ratelimit,
        Pricing pricing
) {
    /**
     * Catálogo de precios del Core. La fuente de verdad es `pricing.md`; el Core la publica ya
     * calculada (UF, neto, con IVA y USD del día). Este servicio la <strong>consume verbatim</strong>:
     * no convierte nada, para no diferir en un peso con lo que muestra el sitio.
     *
     * @param url      endpoint del Core, p.ej. https://core.webiados.com/api/v1/pricing
     * @param timeoutSeconds tope de espera; si el Core no responde se sirve el último cacheado
     */
    public record Pricing(String url, int timeoutSeconds) {
    }

    public record Admin(String bootstrapEmail, String bootstrapPassword) {
    }

    /**
     * @param publicBaseUrl base de la URL que ve el cliente, sin barra final. La landing
     *                      la sirve el frontend Angular, no este servicio. Antes estaba
     *                      escrita a mano dentro de QuoteService.
     */
    public record Quote(int validityDays, String publicBaseUrl) {
    }

    public record Mail(String from, String notifyTo) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Ratelimit(int unlockMaxAttempts, int unlockWindowMinutes) {
    }
}
