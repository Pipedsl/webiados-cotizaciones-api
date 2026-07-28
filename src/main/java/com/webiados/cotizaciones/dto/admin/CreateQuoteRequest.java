package com.webiados.cotizaciones.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

/**
 * @param createdAt fecha de emisión. Normalmente {@code null} (se usa el momento actual);
 *                  se informa solo para cargar cotizaciones históricas con su fecha
 *                  verdadera. Sin esto el histórico nace fechado el día de la migración y
 *                  la tasa de cierre por período queda mal.
 */
public record CreateQuoteRequest(
        @NotBlank String clientName,
        String clientEmail,
        String notes,
        String titulo,
        String mensaje,
        String imagenes,
        Instant createdAt,
        @NotEmpty @Valid List<OptionRequest> options
) {
}
