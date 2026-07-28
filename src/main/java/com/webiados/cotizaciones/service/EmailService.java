package com.webiados.cotizaciones.service;

import com.webiados.cotizaciones.config.AppProperties;
import com.webiados.cotizaciones.domain.Quote;
import com.webiados.cotizaciones.domain.QuoteOption;
import com.webiados.cotizaciones.domain.SelectionKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final AppProperties props;

    public EmailService(JavaMailSender mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    /**
     * Le manda la cotización al cliente: el link y la clave de acceso.
     *
     * <p>Esto es lo que hacía falta para que usar el sistema cueste menos que escribir un
     * Markdown. Hasta ahora el único correo que existía era el aviso interno de más abajo,
     * así que Felipe tenía que copiar código y clave del panel y mandarlos a mano.
     *
     * <p>A diferencia de {@link #notifySelection}, este método <strong>no es async y
     * propaga la excepción</strong>: si el correo no sale, la cotización no puede quedar
     * marcada como enviada. Un SENT sin correo es peor que un error visible.
     */
    public void sendQuoteToClient(Quote quote, String url) {
        if (quote.getClientEmail() == null || quote.getClientEmail().isBlank()) {
            throw new IllegalStateException(
                    "La cotización no tiene correo del cliente: no hay a quién enviarla");
        }
        if (quote.getClaveTexto() == null || quote.getClaveTexto().isBlank()) {
            throw new IllegalStateException(
                    "No se conserva la clave en texto de esta cotización; no se puede enviar. "
                            + "Crea una cotización nueva o entrégala manualmente.");
        }

        String subject = "Tu cotización de Webiados — %s".formatted(
                quote.getTitulo() != null && !quote.getTitulo().isBlank()
                        ? quote.getTitulo()
                        : quote.getClientName());

        String body = """
                Hola %s:

                Preparamos tu cotización. Puedes verla acá:

                %s

                Clave de acceso: %s

                Adentro vas a encontrar las opciones con su detalle y sus valores. Puedes
                elegir la que prefieras desde la misma página.

                La cotización está vigente hasta el %s.

                Cualquier duda, respóndenos este correo.

                Webiados
                https://webiados.com
                """.formatted(
                quote.getClientName(),
                url,
                quote.getClaveTexto(),
                quote.getExpiresAt());

        var message = new SimpleMailMessage();
        message.setFrom(props.mail().from());
        message.setTo(quote.getClientEmail());
        message.setReplyTo(props.mail().notifyTo());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);

        log.info("Cotización {} enviada por correo a {}", quote.getCodigo(), quote.getClientEmail());
    }

    @Async
    public void notifySelection(Quote quote, QuoteOption option, SelectionKind kind) {
        try {
            String subject = kind == SelectionKind.UPGRADE
                    ? "⬆️ Upgrade — Cotización %s — %s".formatted(quote.getCodigo(), quote.getClientName())
                    : "✅ Cotización %s — %s eligió %s".formatted(
                            quote.getCodigo(), quote.getClientName(), option.getTitulo());

            String body = """
                    Cliente: %s
                    Email: %s
                    Código: %s
                    Opción elegida: %s
                    Precio: $%s %s
                    Tipo: %s
                    """.formatted(
                    quote.getClientName(),
                    quote.getClientEmail() != null ? quote.getClientEmail() : "—",
                    quote.getCodigo(),
                    option.getTitulo(),
                    option.getPrecio().toPlainString(),
                    option.getCurrency(),
                    kind == SelectionKind.UPGRADE ? "UPGRADE" : "SELECCIÓN INICIAL");

            var message = new SimpleMailMessage();
            message.setFrom(props.mail().from());
            message.setTo(props.mail().notifyTo());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Error enviando email de notificación para cotización {}", quote.getCodigo(), ex);
        }
    }
}
