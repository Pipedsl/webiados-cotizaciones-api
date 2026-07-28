package com.webiados.cotizaciones.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Montos <strong>netos</strong>, sin IVA. El IVA lo calcula el servicio a partir del
 * porcentaje de la cotización; no se manda ni se guarda ya sumado.
 *
 * @param precio        pago único de instalación / desarrollo
 * @param precioMensual mensualidad recurrente, o {@code null} si la opción no tiene
 */
public record OptionRequest(
        @NotBlank String titulo,
        String descripcion,
        @NotNull @PositiveOrZero BigDecimal precio,
        @PositiveOrZero BigDecimal precioMensual,
        String currency,
        boolean recomendado,
        List<String> features
) {
}
