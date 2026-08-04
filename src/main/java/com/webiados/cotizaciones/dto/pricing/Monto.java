package com.webiados.cotizaciones.dto.pricing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Un monto tal como lo calcula el Core, en las cuatro monedas. <strong>No se convierte acá:</strong>
 * si este servicio convirtiera y el sitio también, tarde o temprano difieren en un peso y la web
 * diría un número distinto al de la cotización. Se pasa verbatim.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Monto(BigDecimal uf, BigDecimal neto, BigDecimal conIva, BigDecimal usd) {
}
