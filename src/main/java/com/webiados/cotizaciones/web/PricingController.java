package com.webiados.cotizaciones.web;

import com.webiados.cotizaciones.dto.pricing.PricingCatalog;
import com.webiados.cotizaciones.service.PricingClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone el catálogo de precios del Core al panel, para que el admin arme opciones eligiendo
 * ítems con el monto prellenado, en vez de teclearlo. Los montos vienen del Core, verbatim.
 */
@RestController
@RequestMapping("/api/admin/pricing")
@PreAuthorize("hasRole('ADMIN')")
public class PricingController {

    private final PricingClient pricing;

    public PricingController(PricingClient pricing) {
        this.pricing = pricing;
    }

    @GetMapping
    public ResponseEntity<PricingCatalog> get() {
        return ResponseEntity.ok(pricing.get());
    }
}
