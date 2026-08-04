package com.webiados.cotizaciones.service;

import com.webiados.cotizaciones.config.AppProperties;
import com.webiados.cotizaciones.dto.lead.Lead;
import com.webiados.cotizaciones.dto.lead.LeadPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Lee leads del CRM del Core ({@code GET /api/v1/leads}) con la llave de tenant. El formulario
 * público NO llega acá: postea al Core. Este servicio solo lee un lead para convertirlo en un
 * borrador de cotización, con los campos del CRM (sin esquema paralelo).
 *
 * <p>No cachea: un lead es dato vivo (su estado cambia), servir uno viejo sería peor que fallar.
 */
@Service
public class LeadClient {

    private final RestClient http;
    private final String url;
    private final String apiKey;

    @Autowired
    public LeadClient(AppProperties props) {
        this(props.leads().url(), props.leads().apiKey(), buildClient(props.leads().timeoutSeconds()));
    }

    /** Para pruebas: URL de un Core simulado, una llave cualquiera, y un RestClient sin timeouts. */
    LeadClient(String url, String apiKey, RestClient http) {
        this.url = url;
        this.apiKey = apiKey;
        this.http = http;
    }

    private static RestClient buildClient(int timeoutSeconds) {
        Duration t = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        var settings = ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(t).withReadTimeout(t);
        return RestClient.builder().requestFactory(ClientHttpRequestFactories.get(settings)).build();
    }

    /** Lista leads del Core, opcionalmente filtrados por estado. */
    public LeadPage list(String estado, int limit) {
        requireApiKey();
        String uri = UriComponentsBuilder.fromUriString(url)
                .queryParamIfPresent("estado",
                        estado == null || estado.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(estado))
                .queryParam("limit", Math.max(1, limit))
                .build().toUriString();
        try {
            LeadPage page = http.get().uri(uri)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve().body(LeadPage.class);
            return page != null ? page : new LeadPage(List.of(), 0, 1, 1);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo leer los leads del Core: " + ex.getMessage(), ex);
        }
    }

    /**
     * Un lead por id. El endpoint del Core no tiene "traer por id", así que se busca entre los
     * leads recientes; si el admin lo eligió de la lista, está ahí.
     */
    public Lead find(long leadId) {
        return list(null, 200).docs().stream()
                .filter(l -> l.id() != null && l.id() == leadId)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "El lead " + leadId + " no está entre los leads recientes del Core"));
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "La conversión de leads requiere la llave del Core (CORE_API_KEY) y no está "
                            + "configurada. Pídesela a Felipe y ponla en Railway.");
        }
    }
}
