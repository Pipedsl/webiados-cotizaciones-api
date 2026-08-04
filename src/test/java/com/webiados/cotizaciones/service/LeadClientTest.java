package com.webiados.cotizaciones.service;

import com.sun.net.httpserver.HttpServer;
import com.webiados.cotizaciones.dto.lead.Lead;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeadClientTest {

    private static final String PAGINA = """
            {"docs":[
              {"id":7,"nombre":"maría pérez","email":"m@ejemplo.cl","telefono":"+56912345678",
               "mensaje":"Quiero una tienda online","interes":{"kit":"tienda"},
               "origen":"formulario","estado":"nuevo","score":10,"etiquetas":["caliente"]}
             ],"total":1,"page":1,"totalPages":1}
            """;

    private final AtomicReference<String> authRecibido = new AtomicReference<>();

    private HttpServer startCore(String json) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/leads", exchange -> {
            authRecibido.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return server;
    }

    private String urlOf(HttpServer s) {
        return "http://127.0.0.1:" + s.getAddress().getPort() + "/api/v1/leads";
    }

    @Test
    void lista_leads_con_la_llave_del_core_y_toma_los_campos_del_crm() throws IOException {
        HttpServer core = startCore(PAGINA);
        try {
            var client = new LeadClient(urlOf(core), "wcore_live_prueba", RestClient.create());

            var page = client.list(null, 50);

            assertThat(authRecibido.get()).isEqualTo("Bearer wcore_live_prueba");
            assertThat(page.docs()).hasSize(1);
            Lead l = page.docs().get(0);
            assertThat(l.id()).isEqualTo(7L);
            assertThat(l.nombre()).isEqualTo("maría pérez");
            assertThat(l.email()).isEqualTo("m@ejemplo.cl");
            assertThat(l.interes().get("kit").asText()).isEqualTo("tienda");
        } finally {
            core.stop(0);
        }
    }

    @Test
    void find_devuelve_el_lead_por_id_y_falla_si_no_esta() throws IOException {
        HttpServer core = startCore(PAGINA);
        try {
            var client = new LeadClient(urlOf(core), "wcore_live_prueba", RestClient.create());

            assertThat(client.find(7).nombre()).isEqualTo("maría pérez");
            assertThatThrownBy(() -> client.find(999))
                    .isInstanceOf(java.util.NoSuchElementException.class);
        } finally {
            core.stop(0);
        }
    }

    @Test
    void sin_llave_del_core_falla_con_mensaje_claro() {
        var client = new LeadClient("http://127.0.0.1:1/api/v1/leads", "", RestClient.create());
        assertThatThrownBy(() -> client.list(null, 50))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORE_API_KEY");
    }
}
