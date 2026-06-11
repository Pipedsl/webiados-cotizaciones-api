package com.webiados.cotizaciones.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "selection")
public class Selection {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private QuoteOption option;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SelectionKind kind;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Selection() {
    }

    public Selection(UUID id, Quote quote, QuoteOption option, SelectionKind kind, Instant createdAt) {
        this.id = id;
        this.quote = quote;
        this.option = option;
        this.kind = kind;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Quote getQuote() {
        return quote;
    }

    public QuoteOption getOption() {
        return option;
    }

    public SelectionKind getKind() {
        return kind;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
