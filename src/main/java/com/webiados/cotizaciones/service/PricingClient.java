package com.webiados.cotizaciones.service;

import com.webiados.cotizaciones.config.AppProperties;
import com.webiados.cotizaciones.dto.pricing.PricingCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

/**
 * Lee el catálogo de precios del Core ({@code GET /api/v1/pricing}) — read-through, no fuente nueva.
 *
 * <p>Reglas que lo mantienen honesto:
 * <ul>
 *   <li><b>No convierte nada.</b> Devuelve los montos del Core verbatim.</li>
 *   <li><b>No inventa precios.</b> Si el Core no responde, sirve el último cacheado y avisa; si
 *       nunca hubo cache, falla en vez de improvisar.</li>
 * </ul>
 */
@Service
public class PricingClient {

    private static final Logger log = LoggerFactory.getLogger(PricingClient.class);

    private final RestClient http;
    private final String url;

    // Último catálogo bueno, para servir si el Core se cae. volatile: lo lee y escribe cualquier hilo.
    private volatile PricingCatalog cache;
    private volatile Instant cachedAt;

    @Autowired
    public PricingClient(AppProperties props) {
        this(props.pricing().url(), buildClient(props.pricing().timeoutSeconds()));
    }

    /** Para pruebas: inyectar la URL de un Core simulado y un RestClient sin timeouts. */
    PricingClient(String url, RestClient http) {
        this.url = url;
        this.http = http;
    }

    private static RestClient buildClient(int timeoutSeconds) {
        Duration t = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(t)
                .withReadTimeout(t);
        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    /**
     * El catálogo del Core. Devuelve el fresco si el Core responde; si falla, el último cacheado.
     *
     * @throws IllegalStateException si el Core no responde y no hay nada cacheado todavía.
     */
    public PricingCatalog get() {
        try {
            PricingCatalog fresh = http.get().uri(url).retrieve().body(PricingCatalog.class);
            if (fresh != null) {
                cache = fresh;
                cachedAt = Instant.now();
            }
            return fresh;
        } catch (Exception ex) {
            if (cache != null) {
                log.warn("El Core no respondió el catálogo de precios; sirvo el cacheado de {}", cachedAt, ex);
                return cache;
            }
            throw new IllegalStateException(
                    "No se pudo leer el catálogo de precios del Core y no hay ninguno cacheado. "
                            + "No se inventa un precio.", ex);
        }
    }
}
