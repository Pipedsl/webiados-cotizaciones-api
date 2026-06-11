package com.webiados.cotizaciones.dto.admin;

import com.webiados.cotizaciones.domain.QuoteStatus;

import java.time.Instant;
import java.util.UUID;

public record QuoteAdminSummary(
        UUID id,
        String codigo,
        String clientName,
        String clientEmail,
        QuoteStatus status,
        String selectedOptionTitulo,
        Instant createdAt,
        Instant expiresAt,
        Instant selectedAt
) {
}
