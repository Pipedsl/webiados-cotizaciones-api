package com.webiados.cotizaciones.dto.pricing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * El catálogo de precios que publica el Core en {@code GET /api/v1/pricing}. Espejo verbatim: este
 * servicio lo consume tal cual, sin recalcular ni convertir. El Core ya filtra las secciones internas
 * de {@code pricing.md} (§10-15), así que acá nunca llega nada privado.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PricingCatalog(
        String moneda,
        Boolean incluyeIva,
        BigDecimal iva,
        Indicadores indicadores,
        String actualizado,
        List<ItemPrecio> landings,
        BigDecimal mantencionLanding,
        Monto mantencionLandingMonto,
        List<ItemPrecio> kits,
        List<ItemPrecio> addons,
        List<ItemPrecio> identidad,
        List<ItemPrecioSimple> piezas,
        List<ItemPrecioSimple> horas
) {
}
