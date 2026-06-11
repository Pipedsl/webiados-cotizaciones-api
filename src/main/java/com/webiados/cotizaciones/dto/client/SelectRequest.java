package com.webiados.cotizaciones.dto.client;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SelectRequest(@NotNull UUID optionId) {
}
