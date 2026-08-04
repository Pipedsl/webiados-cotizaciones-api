package com.webiados.cotizaciones.dto.pricing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** UF y dólar del día con que el Core calculó los montos. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Indicadores(BigDecimal uf, BigDecimal dolar, String fecha, String origen) {
}
