package com.webiados.cotizaciones.dto.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuoteClientView(
        String clientName,
        boolean canSelect,
        boolean isExpired,
        Instant expiresAt,
        UUID selectedOptionId,
        String titulo,
        String mensaje,
        String imagenes,
        int ivaPct,
        List<OptionClientView> options
) {
}
