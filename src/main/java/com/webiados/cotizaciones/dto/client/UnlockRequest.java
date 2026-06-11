package com.webiados.cotizaciones.dto.client;

import jakarta.validation.constraints.NotBlank;

public record UnlockRequest(@NotBlank String clave) {
}
