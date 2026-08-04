package com.webiados.cotizaciones.dto.lead;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Un lead del CRM del Core, tal como lo devuelve {@code GET /api/v1/leads}. Se toma su esquema
 * verbatim (no se inventa uno paralelo). Payload agrega más campos (tenant, score, etiquetas,
 * fechas…) que acá se ignoran: solo interesa lo que sirve para armar el borrador de cotización.
 *
 * @param interes JSON libre del Core ("qué producto/servicio le interesa"). Se pasa como texto a
 *                las notas del borrador; no se interpreta acá.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Lead(
        Long id,
        String nombre,
        String email,
        String telefono,
        String mensaje,
        JsonNode interes,
        String origen,
        String estado
) {
}
