package com.webiados.cotizaciones.dto.admin;

import jakarta.validation.constraints.NotNull;

/** Pedido para convertir un lead del Core en un borrador de cotización. */
public record FromLeadRequest(@NotNull Long leadId) {
}
