package com.webiados.cotizaciones.repo;

import com.webiados.cotizaciones.domain.QuoteOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuoteOptionRepository extends JpaRepository<QuoteOption, UUID> {
}
