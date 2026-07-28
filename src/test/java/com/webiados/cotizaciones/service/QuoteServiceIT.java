package com.webiados.cotizaciones.service;

import com.webiados.cotizaciones.db.TestPostgres;
import com.webiados.cotizaciones.domain.QuoteStatus;
import com.webiados.cotizaciones.dto.admin.CreateQuoteRequest;
import com.webiados.cotizaciones.dto.admin.OptionRequest;
import com.webiados.cotizaciones.dto.admin.UpdateQuoteRequest;
import com.webiados.cotizaciones.repo.QuoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Integración real: Spring completo, Flyway y Postgres embebido.
 *
 * <p>Con {@code ddl-auto: validate}, que este test arranque ya prueba que las entidades
 * JPA calzan con el esquema que dejan las migraciones.
 */
@SpringBootTest
@Import(QuoteServiceIT.PostgresConfig.class)
@TestPropertySource(properties = {
        "app.admin.bootstrap-email=",
        "app.admin.bootstrap-password=",
        "app.quote.public-base-url=https://webiados.com/cotizacion",
})
class QuoteServiceIT {

    @TestConfiguration
    static class PostgresConfig {
        @Bean
        DataSource dataSource() {
            return TestPostgres.freshDatabase();
        }
    }

    @Autowired
    QuoteService quoteService;

    @Autowired
    QuoteRepository quoteRepo;

    @MockBean
    JavaMailSender mailSender;

    private static OptionRequest opcion(String titulo, long precio, Long mensual) {
        return new OptionRequest(titulo, "descripción", BigDecimal.valueOf(precio),
                mensual == null ? null : BigDecimal.valueOf(mensual),
                "CLP", false, List.of("feature"));
    }

    private static CreateQuoteRequest cotizacion(String email) {
        return new CreateQuoteRequest("Pastelería Vientos del Sur", email, null,
                "Tienda online", "mensaje", null, null,
                List.of(opcion("Opción A", 1040000, 49000L),
                        opcion("Opción B", 1240000, 49000L),
                        opcion("Opción C", 1640000, 74000L)));
    }

    @Test
    @DisplayName("una cotización nueva nace PENDING y sin fecha de envío")
    void naceePending() {
        var creada = quoteService.create(cotizacion("cliente@ejemplo.cl"));

        var detalle = quoteService.getDetail(creada.id());
        assertThat(detalle.status()).isEqualTo(QuoteStatus.PENDING);
        assertThat(detalle.sentAt()).isNull();
        assertThat(detalle.options()).hasSize(3);
    }

    @Test
    @DisplayName("la mensualidad se guarda y vuelve con su IVA calculado")
    void mensualidadConIva() {
        var creada = quoteService.create(cotizacion("cliente@ejemplo.cl"));

        var opcionC = quoteService.getDetail(creada.id()).options().get(2);

        assertThat(opcionC.precio()).isEqualByComparingTo("1640000");
        assertThat(opcionC.precioTotal()).isEqualByComparingTo("1951600");
        assertThat(opcionC.precioMensual()).isEqualByComparingTo("74000");
        assertThat(opcionC.precioMensualTotal()).isEqualByComparingTo("88060");
        assertThat(opcionC.ivaPct()).isEqualTo(19);
    }

    @Test
    @DisplayName("enviarla la deja SENT, con fecha, y le manda el correo al cliente")
    void enviarMandaCorreoAlCliente() {
        var creada = quoteService.create(cotizacion("cliente@ejemplo.cl"));

        var detalle = quoteService.send(creada.id());

        assertThat(detalle.status()).isEqualTo(QuoteStatus.SENT);
        assertThat(detalle.sentAt()).isNotNull();

        var mensaje = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mensaje.capture());
        assertThat(mensaje.getValue().getTo()).containsExactly("cliente@ejemplo.cl");
        assertThat(mensaje.getValue().getText())
                .contains("https://webiados.com/cotizacion/" + creada.codigo())
                .contains(creada.clave());
    }

    @Test
    @DisplayName("sin correo del cliente no se puede enviar, y no queda marcada como enviada")
    void sinCorreoNoSeEnvia() {
        var creada = quoteService.create(cotizacion(null));

        assertThatThrownBy(() -> quoteService.send(creada.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("correo");

        assertThat(quoteService.getDetail(creada.id()).status()).isEqualTo(QuoteStatus.PENDING);
    }

    @Test
    @DisplayName("si el correo falla, la cotización NO queda como enviada")
    void correoFallidoNoMarcaSent() {
        var creada = quoteService.create(cotizacion("cliente@ejemplo.cl"));
        doThrow(new MailSendException("SMTP caído")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> quoteService.send(creada.id()))
                .isInstanceOf(MailSendException.class);

        assertThat(quoteRepo.findById(creada.id()).orElseThrow().getStatus())
                .as("una SENT sin correo enviado falsearía la tasa de cierre")
                .isEqualTo(QuoteStatus.PENDING);
    }

    @Test
    @DisplayName("rechazar la deja REJECTED con fecha")
    void rechazar() {
        var creada = quoteService.create(cotizacion("cliente@ejemplo.cl"));
        quoteService.send(creada.id());

        var detalle = quoteService.reject(creada.id());

        assertThat(detalle.status()).isEqualTo(QuoteStatus.REJECTED);
        assertThat(detalle.rejectedAt()).isNotNull();
        assertThat(detalle.sentAt()).as("sigue contando como enviada").isNotNull();
    }

    @Test
    @DisplayName("se puede agregar una opción sin cambiar el código ni la clave")
    void agregarOpcion() {
        var creada = quoteService.create(cotizacion("cliente@ejemplo.cl"));

        var detalle = quoteService.addOption(creada.id(), opcion("Opción D", 2000000, 90000L));

        assertThat(detalle.options()).hasSize(4);
        assertThat(detalle.options().get(3).titulo()).isEqualTo("Opción D");
        assertThat(detalle.codigo()).isEqualTo(creada.codigo());
    }

    @Test
    @DisplayName("un PATCH parcial no borra los campos que no vienen")
    void patchParcialNoBorra() {
        var creada = quoteService.create(cotizacion("cliente@ejemplo.cl"));

        quoteService.updateQuote(creada.id(),
                new UpdateQuoteRequest(null, null, "llamé al cliente", null, null));

        var detalle = quoteService.getDetail(creada.id());
        assertThat(detalle.titulo()).isEqualTo("Tienda online");
        assertThat(detalle.mensaje()).isEqualTo("mensaje");
        assertThat(detalle.notes()).isEqualTo("llamé al cliente");
    }

    @Test
    @DisplayName("borrar una opción normal reindexa el orden")
    void borrarOpcionReindexa() {
        var creada = quoteService.create(cotizacion("cliente@ejemplo.cl"));
        var opciones = quoteService.getDetail(creada.id()).options();

        quoteService.deleteOption(creada.id(), opciones.get(0).id());

        var quedan = quoteService.getDetail(creada.id()).options();
        assertThat(quedan).hasSize(2);
        assertThat(quedan).extracting("orderIndex").containsExactly(0, 1);
    }

    @Test
    @DisplayName("no se puede borrar la opción que el cliente eligió")
    void noSeBorraLaOpcionElegida() {
        var creada = quoteService.create(cotizacion("cliente@ejemplo.cl"));
        var opciones = quoteService.getDetail(creada.id()).options();
        var elegida = opciones.get(1).id();

        var quote = quoteRepo.findById(creada.id()).orElseThrow();
        quote.recordSelection(elegida, java.time.Instant.now());
        quoteRepo.save(quote);

        assertThatThrownBy(() -> quoteService.deleteOption(creada.id(), elegida))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("eligió");

        assertThat(quoteService.getDetail(creada.id()).options()).hasSize(3);
    }

    @Test
    @DisplayName("carga histórica: fecha de emisión y de envío reales, sin mandar correo")
    void cargaHistorica() {
        var emitida = java.time.Instant.parse("2026-07-24T15:00:00Z");
        var req = new CreateQuoteRequest("Macarena Larraín", null, null,
                "Portafolio de charlas", "mensaje", null, emitida,
                List.of(opcion("Opción C", 380000, null)));

        var creada = quoteService.create(req);
        var detalle = quoteService.markSentManually(creada.id(), emitida);

        assertThat(detalle.createdAt()).isEqualTo(emitida);
        assertThat(detalle.sentAt()).isEqualTo(emitida);
        assertThat(detalle.status()).isEqualTo(QuoteStatus.SENT);
        org.mockito.Mockito.verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("no se puede emitir una cotización con fecha futura")
    void noSeEmiteEnElFuturo() {
        var futuro = java.time.Instant.now().plusSeconds(86400);
        var req = new CreateQuoteRequest("Cliente", null, null, null, null, null, futuro,
                List.of(opcion("Opción A", 100000, null)));

        assertThatThrownBy(() -> quoteService.create(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("marcar entregada una ya aceptada conserva SELECTED y registra el envío")
    void marcarEntregadaUnaAceptada() {
        var creada = quoteService.create(cotizacion("cliente@ejemplo.cl"));
        var elegida = quoteService.getDetail(creada.id()).options().get(2).id();
        var quote = quoteRepo.findById(creada.id()).orElseThrow();
        quote.recordSelection(elegida, java.time.Instant.now());
        quoteRepo.save(quote);

        var detalle = quoteService.markSentManually(creada.id(), java.time.Instant.now());

        assertThat(detalle.status())
                .as("que se registre la entrega no deshace la aceptación")
                .isEqualTo(QuoteStatus.SELECTED);
        assertThat(detalle.sentAt()).isNotNull();
    }

    @Test
    @DisplayName("una cotización que no existe da 404, no 500")
    void noExiste() {
        assertThatThrownBy(() -> quoteService.getDetail(java.util.UUID.randomUUID()))
                .isInstanceOf(NoSuchElementException.class);
    }
}
