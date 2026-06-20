package com.webiados.cotizaciones.service;

import com.webiados.cotizaciones.config.AppProperties;
import com.webiados.cotizaciones.domain.Quote;
import com.webiados.cotizaciones.domain.QuoteOption;
import com.webiados.cotizaciones.dto.admin.CreateQuoteRequest;
import com.webiados.cotizaciones.dto.admin.CreateQuoteResponse;
import com.webiados.cotizaciones.dto.admin.OptionRequest;
import com.webiados.cotizaciones.dto.admin.QuoteAdminDetail;
import com.webiados.cotizaciones.dto.admin.QuoteAdminSummary;
import com.webiados.cotizaciones.dto.admin.UpdateQuoteRequest;
import com.webiados.cotizaciones.dto.client.QuoteClientView;
import com.webiados.cotizaciones.repo.QuoteRepository;
import com.webiados.cotizaciones.repo.SelectionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class QuoteService {

    private final QuoteRepository quoteRepo;
    private final SelectionRepository selectionRepo;
    private final PasswordEncoder passwordEncoder;
    private final CodeGenerator codeGenerator;
    private final QuoteMapper mapper;
    private final AppProperties props;

    public QuoteService(QuoteRepository quoteRepo, SelectionRepository selectionRepo,
                        PasswordEncoder passwordEncoder, CodeGenerator codeGenerator,
                        QuoteMapper mapper, AppProperties props) {
        this.quoteRepo = quoteRepo;
        this.selectionRepo = selectionRepo;
        this.passwordEncoder = passwordEncoder;
        this.codeGenerator = codeGenerator;
        this.mapper = mapper;
        this.props = props;
    }

    @Transactional
    public CreateQuoteResponse create(CreateQuoteRequest req) {
        String codigo = codeGenerator.generateCodigo();
        String clave = codeGenerator.generateClave();
        String claveHash = passwordEncoder.encode(clave);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(props.quote().validityDays(), ChronoUnit.DAYS);

        var quote = new Quote(UUID.randomUUID(), codigo, claveHash, clave,
                req.clientName(), req.clientEmail(), req.notes(), now, expiresAt,
                req.titulo(), req.mensaje(), req.imagenes());

        int index = 0;
        for (OptionRequest optReq : req.options()) {
            var option = new QuoteOption(
                    UUID.randomUUID(), index++,
                    optReq.titulo(), optReq.descripcion(),
                    optReq.precio(),
                    optReq.currency() != null ? optReq.currency() : "CLP",
                    optReq.recomendado(),
                    optReq.features()
            );
            quote.addOption(option);
        }

        quoteRepo.save(quote);

        String url = "https://webiados.com/cotizacion/" + codigo;
        return new CreateQuoteResponse(quote.getId(), codigo, clave, url);
    }

    public List<QuoteAdminSummary> listAll() {
        Instant now = Instant.now();
        return quoteRepo.findAllByOrderByCreatedAtDesc().stream().map(q -> {
            Map<String, String> titles = q.getOptions().stream()
                    .collect(Collectors.toMap(o -> o.getId().toString(), QuoteOption::getTitulo));
            return mapper.toSummary(q, now, titles);
        }).toList();
    }

    public QuoteAdminDetail getDetail(UUID id) {
        var quote = quoteRepo.findWithOptionsById(id)
                .orElseThrow(() -> new NoSuchElementException("Cotización no encontrada"));
        var history = selectionRepo.findByQuoteIdOrderByCreatedAtAsc(id);
        return mapper.toDetail(quote, history, Instant.now());
    }

    @Transactional
    public QuoteAdminDetail updateQuote(UUID id, UpdateQuoteRequest req) {
        var quote = quoteRepo.findWithOptionsById(id)
                .orElseThrow(() -> new NoSuchElementException("Cotización no encontrada"));
        quote.updateMeta(req.titulo(), req.mensaje(), req.notes(), req.expiresAt());
        var history = selectionRepo.findByQuoteIdOrderByCreatedAtAsc(id);
        return mapper.toDetail(quote, history, Instant.now());
    }

    @Transactional
    public QuoteAdminDetail updateOption(UUID quoteId, UUID optionId, OptionRequest req) {
        var quote = quoteRepo.findWithOptionsById(quoteId)
                .orElseThrow(() -> new NoSuchElementException("Cotización no encontrada"));
        var option = quote.getOptions().stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Opción no encontrada"));
        option.update(req.titulo(), req.descripcion(), req.precio(),
                req.currency(), req.recomendado(), req.features());
        var history = selectionRepo.findByQuoteIdOrderByCreatedAtAsc(quoteId);
        return mapper.toDetail(quote, history, Instant.now());
    }

    @Transactional
    public void deleteOption(UUID quoteId, UUID optionId) {
        var quote = quoteRepo.findWithOptionsById(quoteId)
                .orElseThrow(() -> new NoSuchElementException("Cotización no encontrada"));
        boolean removed = quote.getOptions().removeIf(o -> o.getId().equals(optionId));
        if (!removed) throw new NoSuchElementException("Opción no encontrada");
    }

    public Quote findByCodigo(String codigo) {
        return quoteRepo.findByCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException("Cotización no encontrada"));
    }

    public QuoteClientView getClientViewByCodigo(String codigo) {
        var quote = quoteRepo.findByCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException("Cotización no encontrada"));
        return mapper.toClientView(quote, Instant.now());
    }
}
