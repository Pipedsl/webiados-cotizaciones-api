package com.webiados.cotizaciones.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quote_option")
public class QuoteOption {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 3)
    private String currency = "CLP";

    @Column(nullable = false)
    private boolean recomendado = false;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "quote_option_feature", joinColumns = @JoinColumn(name = "option_id"))
    @OrderColumn(name = "position")
    @Column(name = "feature", length = 500, nullable = false)
    private List<String> features = new ArrayList<>();

    protected QuoteOption() {
    }

    public QuoteOption(UUID id, int orderIndex, String titulo, String descripcion,
                       BigDecimal precio, String currency, boolean recomendado,
                       List<String> features) {
        this.id = id;
        this.orderIndex = orderIndex;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.precio = precio;
        this.currency = currency != null ? currency : "CLP";
        this.recomendado = recomendado;
        this.features = features != null ? new ArrayList<>(features) : new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public Quote getQuote() {
        return quote;
    }

    void setQuote(Quote quote) {
        this.quote = quote;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isRecomendado() {
        return recomendado;
    }

    public List<String> getFeatures() {
        return features;
    }
}
