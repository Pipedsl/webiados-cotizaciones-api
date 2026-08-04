package com.webiados.cotizaciones.dto.lead;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Página de leads que devuelve el Core: {@code { docs, total, page, totalPages }}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeadPage(
        List<Lead> docs,
        Integer total,
        Integer page,
        Integer totalPages
) {
}
