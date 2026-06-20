package com.webiados.cotizaciones.service;

import com.webiados.cotizaciones.domain.QuoteOption;
import com.webiados.cotizaciones.domain.Selection;
import com.webiados.cotizaciones.domain.SelectionKind;
import com.webiados.cotizaciones.dto.client.QuoteClientView;
import com.webiados.cotizaciones.repo.QuoteOptionRepository;
import com.webiados.cotizaciones.repo.QuoteRepository;
import com.webiados.cotizaciones.repo.SelectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class SelectionService {

    private final QuoteRepository quoteRepo;
    private final QuoteOptionRepository optionRepo;
    private final SelectionRepository selectionRepo;
    private final QuoteMapper mapper;
    private final EmailService emailService;

    public SelectionService(QuoteRepository quoteRepo, QuoteOptionRepository optionRepo,
                            SelectionRepository selectionRepo, QuoteMapper mapper,
                            EmailService emailService) {
        this.quoteRepo = quoteRepo;
        this.optionRepo = optionRepo;
        this.selectionRepo = selectionRepo;
        this.mapper = mapper;
        this.emailService = emailService;
    }

    @Transactional
    public QuoteClientView select(String codigo, UUID optionId) {
        Instant now = Instant.now();

        var quote = quoteRepo.findByCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException("Cotización no encontrada"));

        if (!quote.canSelect(now)) {
            throw new IllegalStateException("Esta cotización ha expirado");
        }

        QuoteOption option = optionRepo.findById(optionId)
                .orElseThrow(() -> new NoSuchElementException("Opción no encontrada"));

        if (!option.getQuote().getId().equals(quote.getId())) {
            throw new IllegalArgumentException("La opción no pertenece a esta cotización");
        }

        SelectionKind kind = quote.getSelectedOptionId() == null
                ? SelectionKind.INITIAL
                : SelectionKind.UPGRADE;

        var selection = new Selection(UUID.randomUUID(), quote, option, kind, now);
        selectionRepo.save(selection);
        quote.recordSelection(optionId, now);
        quoteRepo.save(quote);

        emailService.notifySelection(quote, option, kind);

        return mapper.toClientView(quote, now);
    }
}
