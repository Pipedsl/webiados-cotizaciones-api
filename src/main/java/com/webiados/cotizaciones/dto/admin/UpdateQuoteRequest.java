package com.webiados.cotizaciones.dto.admin;

import java.time.Instant;

public record UpdateQuoteRequest(
        String titulo,
        String mensaje,
        String notes,
        Instant expiresAt
) {}
