package com.webiados.cotizaciones.dto.admin;

import com.webiados.cotizaciones.domain.QuoteStatus;
import com.webiados.cotizaciones.dto.client.OptionClientView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuoteAdminDetail(
        UUID id,
        String codigo,
        String claveTexto,
        String clientName,
        String clientEmail,
        String notes,
        String titulo,
        String mensaje,
        String imagenes,
        QuoteStatus status,
        boolean canSelect,
        Instant createdAt,
        Instant expiresAt,
        Instant sentAt,
        UUID selectedOptionId,
        Instant selectedAt,
        Instant rejectedAt,
        int ivaPct,
        List<OptionClientView> options,
        List<SelectionHistoryEntry> history
) {
}
