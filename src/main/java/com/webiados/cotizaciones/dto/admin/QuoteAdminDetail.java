package com.webiados.cotizaciones.dto.admin;

import com.webiados.cotizaciones.domain.QuoteStatus;
import com.webiados.cotizaciones.dto.client.OptionClientView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuoteAdminDetail(
        UUID id,
        String codigo,
        String clientName,
        String clientEmail,
        String notes,
        QuoteStatus status,
        boolean canSelect,
        Instant createdAt,
        Instant expiresAt,
        UUID selectedOptionId,
        Instant selectedAt,
        List<OptionClientView> options,
        List<SelectionHistoryEntry> history
) {
}
