package com.webiados.cotizaciones.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

/**
 * @param createdAt fecha de emisión. Normalmente {@code null} (se usa el momento actual);
 *                  se informa solo para cargar cotizaciones históricas con su fecha
 *                  verdadera. Sin esto el histórico nace fechado el día de la migración y
 *                  la tasa de cierre por período queda mal.
 * @param options   opciones de la cotización. Puede venir <strong>vacía o nula</strong>: una
 *                  cotización nacida de un lead es un borrador que todavía no tiene opciones ni
 *                  precios; el vendedor las agrega después. No se puede <em>enviar</em> un
 *                  borrador sin opciones (eso lo valida {@code QuoteService.send}).
 */
public record CreateQuoteRequest(
        @NotBlank String clientName,
        String clientEmail,
        String notes,
        String titulo,
        String mensaje,
        String imagenes,
        Instant createdAt,
        @Valid List<OptionRequest> options
) {
}
