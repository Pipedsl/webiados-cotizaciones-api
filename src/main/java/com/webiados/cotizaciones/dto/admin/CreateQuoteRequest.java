package com.webiados.cotizaciones.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateQuoteRequest(
        @NotBlank String clientName,
        String clientEmail,
        String notes,
        @NotEmpty @Valid List<OptionRequest> options
) {
}
