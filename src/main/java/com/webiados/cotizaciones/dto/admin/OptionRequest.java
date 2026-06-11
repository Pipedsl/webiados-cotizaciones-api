package com.webiados.cotizaciones.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record OptionRequest(
        @NotBlank String titulo,
        String descripcion,
        @NotNull @PositiveOrZero BigDecimal precio,
        String currency,
        boolean recomendado,
        List<String> features
) {
}
