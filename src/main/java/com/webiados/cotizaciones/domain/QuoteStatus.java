package com.webiados.cotizaciones.domain;

/**
 * Estado derivado (no se persiste). Se calcula a partir de expiresAt + selectedOptionId.
 */
public enum QuoteStatus {
    PENDING,
    SELECTED,
    EXPIRED
}
