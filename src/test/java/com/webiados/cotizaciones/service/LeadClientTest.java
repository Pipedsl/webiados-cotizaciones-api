package com.webiados.cotizaciones.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.webiados.cotizaciones.dto.lead.Lead;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Core simulado en proceso. Nunca se llama al Core de producción.
 *
 * <p>El simulado imita el detalle que importa: <strong>la lista solo trae los más recientes</strong>.
 * El lead 7 existe, pero quedó fuera de la página (como pasará en cuanto el CRM pase de 200). Si
 * alguien revierte {@code find} a "listar y filtrar", los tests se ponen rojos.
 */
class LeadClientTest {

    /** Página de leads recientes: el 7 NO está acá. Es un lead viejo. */
    private static final String PAGINA_RECIENTES = """
            {"docs":[
              {"id":301,"nombre":"lead nuevo","email":"nuevo@ejemplo.cl","origen":"formulario","estado":"nuevo"}
             ],"total":301,"page":1,"totalPages":301}
            """;

    private static final String LEAD_7 = """
            {"id":7,"nombre":"maría pérez","email":"m@ejemplo.cl","telefono":"+56912345678",
             "mensaje":"Quiero una tienda online","interes":{"kit":"tienda"},
             "origen":"formulario","estado":"nuevo","score":10,"etiquetas":["caliente"]}
            """;

    private static final String NO_ENCONTRADO = """
            {"type":"about:blank","title":"No encontrado","status":404,
             "detail":"El lead no existe en este sitio"}
            """;

    private final AtomicReference<String> authRecibido = new AtomicReference<>();
    private final List<String> rutasPedidas = new ArrayList<>();
    private HttpServer core;

    @AfterEach
    void apagarCore() {
        if (core != null) core.stop(0);
    }

    /**
     * Un Core que responde la lista en {@code /api/v1/leads} y un lead puntual en
     * {@code /api/v1/leads/{id}}: 200 para el 7, 404 problem+json para cualquier otro.
     */
    private String coreSimulado() throws IOException {
        core = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        core.createContext("/api/v1/leads", exchange -> {
            authRecibido.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String path = exchange.getRequestURI().getPath();
            rutasPedidas.add(path);
            if (path.equals("/api/v1/leads")) {
                responder(exchange, 200, PAGINA_RECIENTES, "application/json");
            } else if (path.equals("/api/v1/leads/7")) {
                responder(exchange, 200, LEAD_7, "application/json");
            } else {
                responder(exchange, 404, NO_ENCONTRADO, "application/problem+json");
            }
        });
        core.start();
        return "http://127.0.0.1:" + core.getAddress().getPort() + "/api/v1/leads";
    }

    private static void responder(HttpExchange exchange, int status, String json, String tipo) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", tipo + "; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private LeadClient clienteContra(String url) {
        return new LeadClient(url, "wcore_live_prueba", RestClient.create());
    }

    @Test
    void lista_leads_con_la_llave_del_core_y_toma_los_campos_del_crm() throws IOException {
        var client = clienteContra(coreSimulado());

        var page = client.list(null, 50);

        assertThat(authRecibido.get()).isEqualTo("Bearer wcore_live_prueba");
        assertThat(page.docs()).hasSize(1);
        assertThat(page.total()).isEqualTo(301);
    }

    @Test
    void find_trae_el_lead_por_id_aunque_no_esté_entre_los_recientes() throws IOException {
        var client = clienteContra(coreSimulado());

        Lead l = client.find(7);

        assertThat(l.id()).isEqualTo(7L);
        assertThat(l.nombre()).isEqualTo("maría pérez");
        assertThat(l.email()).isEqualTo("m@ejemplo.cl");
        assertThat(l.telefono()).isEqualTo("+56912345678");
        assertThat(l.interes().get("kit").asText()).isEqualTo("tienda");
        assertThat(authRecibido.get()).isEqualTo("Bearer wcore_live_prueba");
    }

    /** El seguro contra la regresión: pedir por id, no listar y filtrar. */
    @Test
    void find_pide_el_lead_puntual_y_no_lista_nada() throws IOException {
        var client = clienteContra(coreSimulado());

        client.find(7);

        assertThat(rutasPedidas).containsExactly("/api/v1/leads/7");
        assertThat(rutasPedidas).doesNotContain("/api/v1/leads");
    }

    @Test
    void un_404_del_core_se_traduce_en_un_error_entendible() throws IOException {
        var client = clienteContra(coreSimulado());

        assertThatThrownBy(() -> client.find(999))
                .isInstanceOf(LeadNoEncontradoException.class)
                .hasMessageContaining("999")
                .hasMessageContaining("no existe en el CRM del Core")
                // No delata si el lead existe en otro tenant: el Core no lo distingue y acá tampoco.
                .hasMessageContaining("o es de otro sitio");
    }

    @Test
    void un_core_caído_no_se_confunde_con_un_lead_inexistente() throws IOException {
        core = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        core.createContext("/api/v1/leads", exchange -> responder(exchange, 500, "{}", "application/json"));
        core.start();
        var client = clienteContra("http://127.0.0.1:" + core.getAddress().getPort() + "/api/v1/leads");

        assertThatThrownBy(() -> client.find(7))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(LeadNoEncontradoException.class)
                .hasMessageContaining("No se pudo leer el lead 7 del Core");
    }

    @Test
    void una_barra_de_más_en_la_url_no_rompe_la_ruta() throws IOException {
        var client = clienteContra(coreSimulado() + "/");

        client.find(7);

        assertThat(rutasPedidas).containsExactly("/api/v1/leads/7");
    }

    @Test
    void sin_llave_del_core_falla_con_mensaje_claro() {
        var client = new LeadClient("http://127.0.0.1:1/api/v1/leads", "", RestClient.create());

        assertThatThrownBy(() -> client.list(null, 50))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORE_API_KEY");
        assertThatThrownBy(() -> client.find(7))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORE_API_KEY");
    }
}
