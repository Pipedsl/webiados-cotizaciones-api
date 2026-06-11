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
