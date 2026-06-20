package com.webiados.cotizaciones.service;

import com.webiados.cotizaciones.domain.Quote;
import com.webiados.cotizaciones.domain.QuoteOption;
import com.webiados.cotizaciones.domain.Selection;
import com.webiados.cotizaciones.dto.admin.QuoteAdminDetail;
import com.webiados.cotizaciones.dto.admin.QuoteAdminSummary;
import com.webiados.cotizaciones.dto.admin.SelectionHistoryEntry;
import com.webiados.cotizaciones.dto.client.OptionClientView;
import com.webiados.cotizaciones.dto.client.QuoteClientView;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QuoteMapper {

    public QuoteClientView toClientView(Quote quote, Instant now) {
        var options = quote.getOptions().stream().map(this::toOptionView).toList();
        return new QuoteClientView(
                quote.getClientName(),
                quote.canSelect(now),
                quote.isExpired(now),
                quote.getExpiresAt(),
                quote.getSelectedOptionId(),
                quote.getTitulo(),
                quote.getMensaje(),
                quote.getImagenes(),
                options
        );
    }

    public QuoteAdminSummary toSummary(Quote quote, Instant now, Map<String, String> optionTituloById) {
        String selectedTitulo = quote.getSelectedOptionId() != null
                ? optionTituloById.get(quote.getSelectedOptionId().toString())
                : null;
        return new QuoteAdminSummary(
                quote.getId(),
                quote.getCodigo(),
                quote.getClientName(),
                quote.getClientEmail(),
                quote.statusAt(now),
                selectedTitulo,
                quote.getCreatedAt(),
                quote.getExpiresAt(),
                quote.getSelectedAt()
        );
    }

    public QuoteAdminDetail toDetail(Quote quote, List<Selection> history, Instant now) {
        var options = quote.getOptions().stream().map(this::toOptionView).toList();
        var optionMap = quote.getOptions().stream()
                .collect(Collectors.toMap(o -> o.getId().toString(), QuoteOption::getTitulo));
        var historyEntries = history.stream().map(s -> new SelectionHistoryEntry(
                s.getId(),
                s.getOption().getId(),
                optionMap.getOrDefault(s.getOption().getId().toString(), "—"),
                s.getKind(),
                s.getCreatedAt()
        )).toList();
        return new QuoteAdminDetail(
                quote.getId(),
                quote.getCodigo(),
                quote.getClaveTexto(),
                quote.getClientName(),
                quote.getClientEmail(),
                quote.getNotes(),
                quote.getTitulo(),
                quote.getMensaje(),
                quote.getImagenes(),
                quote.statusAt(now),
                quote.canSelect(now),
                quote.getCreatedAt(),
                quote.getExpiresAt(),
                quote.getSelectedOptionId(),
                quote.getSelectedAt(),
                options,
                historyEntries
        );
    }

    OptionClientView toOptionView(QuoteOption opt) {
        return new OptionClientView(
                opt.getId(),
                opt.getOrderIndex(),
                opt.getTitulo(),
                opt.getDescripcion(),
                opt.getPrecio(),
                opt.getCurrency(),
                opt.isRecomendado(),
                List.copyOf(opt.getFeatures())
        );
    }
}
