package com.webiados.cotizaciones.repo;

import com.webiados.cotizaciones.domain.Quote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    @EntityGraph(attributePaths = {"options", "options.features"})
    Optional<Quote> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"options", "options.features"})
    Optional<Quote> findWithOptionsById(UUID id);

    boolean existsByCodigo(String codigo);

    List<Quote> findAllByOrderByCreatedAtDesc();
}
