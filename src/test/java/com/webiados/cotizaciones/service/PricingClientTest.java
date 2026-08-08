package com.webiados.cotizaciones.service;

import com.sun.net.httpserver.HttpServer;
import com.webiados.cotizaciones.dto.pricing.ItemPrecio;
import com.webiados.cotizaciones.dto.pricing.PricingCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingClientTest {

    // Muestra recortada pero real de GET /api/v1/pricing del Core.
    private static final String CATALOGO = """
            {
              "moneda":"CLP","incluyeIva":false,"iva":0.19,
              "indicadores":{"uf":40844.79,"dolar":928.42,"fecha":"2026-08-03","origen":"api"},
              "actualizado":"2026-07-31",
              "landings":[],
              "kits":[
                {"nombre":"Tienda","setup":890000,"mensual":49000,
                 "setupMonto":{"uf":21.7898,"neto":890000,"conIva":1059100,"usd":1141},
                 "mensualMonto":{"uf":1.1997,"neto":49000,"conIva":58310,"usd":63},
                 "primerAnioMonto":{"uf":36.1858,"neto":1478000,"conIva":1758820,"usd":1894}}
              ],
              "addons":[
                {"slug":"agenda","etiqueta":"Módulo de reservas","setup":250000,"mensual":15000,
                 "setupMonto":{"uf":6.1207,"neto":250000,"conIva":297500,"usd":320},
                 "mensualMonto":{"uf":0.3672,"neto":15000,"conIva":17850,"usd":19}}
              ],
              "identidad":[
                {"nombre":"Logo","setup":180000,"mensual":0,
                 "setupMonto":{"uf":4.4069,"neto":180000,"conIva":214200,"usd":234},
                 "mensualMonto":{"uf":0,"neto":0,"conIva":0,"usd":0},
                 "primerAnioMonto":{"uf":4.4069,"neto":180000,"conIva":214200,"usd":234}}
              ],
              "piezas":[
                {"nombre":"Set de íconos personalizados (hasta 8)","precio":120000,
                 "precioMonto":{"uf":2.938,"neto":120000,"conIva":142800,"usd":156}}
              ],
              "horas":[
                {"nombre":"Hora de desarrollo","precio":35000,
                 "precioMonto":{"uf":0.8569,"neto":35000,"conIva":41650,"usd":46}}
              ]
            }
            """;

    private HttpServer startCore(String json) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/pricing", exchange -> {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return server;
    }

    private String urlOf(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/pricing";
    }

    @Test
    void lee_el_catalogo_del_core_y_pasa_los_montos_verbatim() throws IOException {
        HttpServer core = startCore(CATALOGO);
        try {
            var client = new PricingClient(urlOf(core), RestClient.create());

            PricingCatalog cat = client.get();

            assertThat(cat.moneda()).isEqualTo("CLP");
            assertThat(cat.incluyeIva()).isFalse();
            assertThat(cat.iva()).isEqualByComparingTo("0.19");
            assertThat(cat.indicadores().uf()).isEqualByComparingTo("40844.79");
            assertThat(cat.indicadores().dolar()).isEqualByComparingTo("928.42");

            ItemPrecio tienda = cat.kits().get(0);
            assertThat(tienda.nombre()).isEqualTo("Tienda");
            assertThat(tienda.setup()).isEqualByComparingTo("890000");
            // Verbatim: el conIva y el primerAnio vienen del Core, NO se recalculan acá.
            assertThat(tienda.setupMonto().conIva()).isEqualByComparingTo("1059100");
            assertThat(tienda.primerAnioMonto().conIva()).isEqualByComparingTo("1758820");
            assertThat(tienda.primerAnioMonto().uf()).isEqualByComparingTo("36.1858");

            ItemPrecio agenda = cat.addons().get(0);
            assertThat(agenda.slug()).isEqualTo("agenda");
            assertThat(agenda.etiqueta()).isEqualTo("Módulo de reservas");
            assertThat(agenda.setupMonto().conIva()).isEqualByComparingTo("297500");
        } finally {
            core.stop(0);
        }
    }

    @Test
    void si_el_core_se_cae_sirve_el_ultimo_cacheado() throws IOException {
        HttpServer core = startCore(CATALOGO);
        var client = new PricingClient(urlOf(core), RestClient.create());
        client.get();          // primer llamado: cachea
        core.stop(0);          // el Core se cae

        PricingCatalog cat = client.get();   // debe servir el cacheado, no fallar

        assertThat(cat.kits().get(0).nombre()).isEqualTo("Tienda");
    }

    @Test
    void si_el_core_nunca_respondio_falla_en_vez_de_inventar() {
        // Puerto muerto, sin cache previo.
        var client = new PricingClient("http://127.0.0.1:1/api/v1/pricing", RestClient.create());

        assertThatThrownBy(client::get)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se inventa");
    }

    @Test
    void expone_identidad_piezas_y_horas() throws IOException {
        HttpServer core = startCore(CATALOGO);
        try {
            var client = new PricingClient(urlOf(core), RestClient.create());

            PricingCatalog cat = client.get();

            // identidad reusa ItemPrecio (nombre + setup)
            assertThat(cat.identidad()).hasSize(1);
            assertThat(cat.identidad().get(0).nombre()).isEqualTo("Logo");
            assertThat(cat.identidad().get(0).setupMonto().conIva()).isEqualByComparingTo("214200");

            // piezas/horas: precio suelto, sin setup/mensual
            assertThat(cat.piezas()).hasSize(1);
            assertThat(cat.piezas().get(0).nombre()).isEqualTo("Set de íconos personalizados (hasta 8)");
            assertThat(cat.piezas().get(0).precio()).isEqualByComparingTo("120000");
            assertThat(cat.piezas().get(0).precioMonto().conIva()).isEqualByComparingTo("142800");

            assertThat(cat.horas()).hasSize(1);
            assertThat(cat.horas().get(0).precioMonto().neto()).isEqualByComparingTo("35000");
        } finally {
            core.stop(0);
        }
    }
}
