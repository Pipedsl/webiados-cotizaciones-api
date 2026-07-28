package com.webiados.cotizaciones.dto.admin;

import java.time.Instant;

/**
 * Registra que la cotización se entregó al cliente <strong>fuera</strong> del sistema
 * (por WhatsApp, en una reunión, o —como el histórico— en un PDF hecho a mano), sin
 * volver a enviarle un correo.
 *
 * @param sentAt fecha real de envío. Si es {@code null} se usa el momento actual.
 *               Permite cargar el histórico con su fecha verdadera, que es lo que hace
 *               que la tasa de cierre signifique algo.
 */
public record MarkSentRequest(Instant sentAt) {
}
