package com.webiados.cotizaciones.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quote")
public class Quote {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String codigo;

    @Column(name = "clave_hash", nullable = false)
    private String claveHash;

    @Column(name = "clave_texto", length = 64)
    private String claveTexto;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_email")
    private String clientEmail;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "selected_option_id")
    private UUID selectedOptionId;

    @Column(name = "selected_at")
    private Instant selectedAt;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuoteOption> options = new ArrayList<>();

    protected Quote() {
    }

    public Quote(UUID id, String codigo, String claveHash, String claveTexto, String clientName,
                 String clientEmail, String notes, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.codigo = codigo;
        this.claveHash = claveHash;
        this.claveTexto = claveTexto;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.notes = notes;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public void addOption(QuoteOption option) {
        option.setQuote(this);
        this.options.add(option);
    }

    /** Expirada solo si nunca se aceptó. Tras aceptar queda viva para upgrades. */
    public boolean isExpired(Instant now) {
        return selectedOptionId == null && now.isAfter(expiresAt);
    }

    public boolean canSelect(Instant now) {
        return selectedOptionId != null || !now.isAfter(expiresAt);
    }

    public QuoteStatus statusAt(Instant now) {
        if (selectedOptionId != null) {
            return QuoteStatus.SELECTED;
        }
        return now.isAfter(expiresAt) ? QuoteStatus.EXPIRED : QuoteStatus.PENDING;
    }

    public void recordSelection(UUID optionId, Instant when) {
        this.selectedOptionId = optionId;
        this.selectedAt = when;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getClaveHash() {
        return claveHash;
    }

    public String getClaveTexto() {
        return claveTexto;
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public UUID getSelectedOptionId() {
        return selectedOptionId;
    }

    public Instant getSelectedAt() {
        return selectedAt;
    }

    public List<QuoteOption> getOptions() {
        return options;
    }
}
