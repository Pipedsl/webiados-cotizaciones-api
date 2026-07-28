package com.webiados.cotizaciones.dto.admin;

import java.time.Instant;

/**
 * Actualización parcial. Un campo en {@code null} significa <em>no lo toques</em>.
 * Antes cualquier campo omitido se escribía como {@code null} y borraba el dato.
 */
public record UpdateQuoteRequest(
        String titulo,
        String mensaje,
        String notes,
        String imagenes,
        Instant expiresAt
) {}
