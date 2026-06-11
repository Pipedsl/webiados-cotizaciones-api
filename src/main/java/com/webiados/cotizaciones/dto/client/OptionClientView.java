package com.webiados.cotizaciones.dto.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OptionClientView(
        UUID id,
        int orderIndex,
        String titulo,
        String descripcion,
        BigDecimal precio,
        String currency,
        boolean recomendado,
        List<String> features
) {
}
