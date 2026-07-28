package com.webiados.cotizaciones.web;

import com.webiados.cotizaciones.dto.admin.CreateQuoteRequest;
import com.webiados.cotizaciones.dto.admin.CreateQuoteResponse;
import com.webiados.cotizaciones.dto.admin.MarkSentRequest;
import com.webiados.cotizaciones.dto.admin.OptionRequest;
import com.webiados.cotizaciones.dto.admin.QuoteAdminDetail;
import com.webiados.cotizaciones.dto.admin.QuoteAdminSummary;
import com.webiados.cotizaciones.dto.admin.UpdateQuoteRequest;
import com.webiados.cotizaciones.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/quotes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuoteController {

    private final QuoteService quoteService;

    public AdminQuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping
    public ResponseEntity<CreateQuoteResponse> create(@Valid @RequestBody CreateQuoteRequest req) {
        return ResponseEntity.ok(quoteService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<QuoteAdminSummary>> list() {
        return ResponseEntity.ok(quoteService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuoteAdminDetail> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(quoteService.getDetail(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<QuoteAdminDetail> update(
            @PathVariable UUID id,
            @RequestBody UpdateQuoteRequest req) {
        return ResponseEntity.ok(quoteService.updateQuote(id, req));
    }

    /**
     * Envía la cotización al cliente por correo y la deja en estado SENT con su fecha.
     * Si el correo falla, no queda marcada como enviada.
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<QuoteAdminDetail> send(@PathVariable UUID id) {
        return ResponseEntity.ok(quoteService.send(id));
    }

    /**
     * Registra que la cotización se entregó fuera del sistema (WhatsApp, reunión, PDF),
     * con su fecha real y sin mandar correo.
     */
    @PostMapping("/{id}/mark-sent")
    public ResponseEntity<QuoteAdminDetail> markSent(
            @PathVariable UUID id,
            @RequestBody(required = false) MarkSentRequest req) {
        return ResponseEntity.ok(
                quoteService.markSentManually(id, req != null ? req.sentAt() : null));
    }

    /** Registra que el cliente rechazó la cotización. */
    @PostMapping("/{id}/reject")
    public ResponseEntity<QuoteAdminDetail> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(quoteService.reject(id));
    }

    /** Agrega una opción a una cotización existente, sin cambiarle el código ni la clave. */
    @PostMapping("/{id}/options")
    public ResponseEntity<QuoteAdminDetail> addOption(
            @PathVariable UUID id,
            @Valid @RequestBody OptionRequest req) {
        return ResponseEntity.ok(quoteService.addOption(id, req));
    }

    @PutMapping("/{id}/options/{optionId}")
    public ResponseEntity<QuoteAdminDetail> updateOption(
            @PathVariable UUID id,
            @PathVariable UUID optionId,
            @Valid @RequestBody OptionRequest req) {
        return ResponseEntity.ok(quoteService.updateOption(id, optionId, req));
    }

    @DeleteMapping("/{id}/options/{optionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOption(
            @PathVariable UUID id,
            @PathVariable UUID optionId) {
        quoteService.deleteOption(id, optionId);
    }
}
