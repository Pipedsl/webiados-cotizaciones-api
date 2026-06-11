package com.webiados.cotizaciones.web;

import com.webiados.cotizaciones.dto.admin.CreateQuoteRequest;
import com.webiados.cotizaciones.dto.admin.CreateQuoteResponse;
import com.webiados.cotizaciones.dto.admin.QuoteAdminDetail;
import com.webiados.cotizaciones.dto.admin.QuoteAdminSummary;
import com.webiados.cotizaciones.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
