package com.webiados.cotizaciones.dto.pricing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Un ítem del catálogo con <strong>precio suelto</strong> (pago único), sin setup/mensual. Es la
 * forma de {@code piezas} y {@code horas} en el Core: {@code { nombre, precio, precioMonto }}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemPrecioSimple(String nombre, BigDecimal precio, Monto precioMonto) {
}
