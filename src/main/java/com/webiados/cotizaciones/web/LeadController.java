package com.webiados.cotizaciones.web;

import com.webiados.cotizaciones.dto.lead.LeadPage;
import com.webiados.cotizaciones.service.LeadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lista los leads del CRM del Core para que el admin elija cuál convertir en cotización.
 * La conversión en sí es {@code POST /api/admin/quotes/from-lead}.
 */
@RestController
@RequestMapping("/api/admin/leads")
@PreAuthorize("hasRole('ADMIN')")
public class LeadController {

    private final LeadService leads;

    public LeadController(LeadService leads) {
        this.leads = leads;
    }

    @GetMapping
    public ResponseEntity<LeadPage> list(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(leads.listar(estado, limit));
    }
}
