package com.webiados.cotizaciones.repo;

import com.webiados.cotizaciones.domain.Selection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SelectionRepository extends JpaRepository<Selection, UUID> {

    List<Selection> findByQuoteIdOrderByCreatedAtAsc(UUID quoteId);

    long countByQuoteId(UUID quoteId);
}
