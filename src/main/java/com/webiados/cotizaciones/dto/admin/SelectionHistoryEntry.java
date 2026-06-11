package com.webiados.cotizaciones.dto.admin;

import com.webiados.cotizaciones.domain.SelectionKind;

import java.time.Instant;
import java.util.UUID;

public record SelectionHistoryEntry(
        UUID selectionId,
        UUID optionId,
        String optionTitulo,
        SelectionKind kind,
        Instant createdAt
) {
}
