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

/**
 * Lee leads del CRM del Core ({@code GET /api/v1/leads} para la lista, {@code GET /api/v1/leads/{id}}
 * para uno puntual) con la llave de tenant. El tenant sale de la llave, nunca de la URL. El formulario
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
        // Sin barra final: `find` le pega el id como último segmento y una barra de más en la
        // config se convertiría en `/leads//7`.
        this.url = url == null ? null : url.replaceAll("/+$", "");
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
     * Un lead por id, contra {@code GET /api/v1/leads/{id}} del Core.
     *
     * <p>Antes esto listaba los 200 leads más recientes y filtraba en memoria, porque el Core no
     * tenía cómo pedir uno puntual. Era una bomba de tiempo con fecha de detonación proporcional al
     * éxito comercial: pasado el lead número 200 —el formulario, el bot de WhatsApp y las demos
     * suman— cualquier prospecto de hace unos días dejaba de ser convertible.
     *
     * @throws LeadNoEncontradoException si el Core responde 404 (no existe, o es de otro tenant:
     *                                   el Core no distingue los dos casos y acá tampoco se asume).
     * @throws IllegalStateException     si falta la llave o el Core no responde bien.
     */
    public Lead find(long leadId) {
        requireApiKey();
        String uri = UriComponentsBuilder.fromUriString(url)
                .pathSegment(Long.toString(leadId))
                .build().toUriString();
        Lead lead;
        try {
            lead = http.get().uri(uri)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new LeadNoEncontradoException(leadId);
                    })
                    .body(Lead.class);
        } catch (LeadNoEncontradoException ex) {
            throw ex;
        } catch (Exception ex) {
            // Cualquier otro fallo (red, 500, JSON ilegible) NO es "el lead no existe": decirlo así
            // mandaría al vendedor a buscar un lead que sí está.
            throw new IllegalStateException(
                    "No se pudo leer el lead " + leadId + " del Core: " + ex.getMessage(), ex);
        }
        if (lead == null) throw new LeadNoEncontradoException(leadId);
        return lead;
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "La conversión de leads requiere la llave del Core (CORE_API_KEY) y no está "
                            + "configurada. Pídesela a Felipe y ponla en Railway.");
        }
    }
}
