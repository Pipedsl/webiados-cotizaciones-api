package com.webiados.cotizaciones.dto.admin;

import java.util.UUID;

public record CreateQuoteResponse(
        UUID id,
        String codigo,
        String clave,   // plaintext, se devuelve una sola vez
        String url      // URL para entregar al cliente
) {
}
