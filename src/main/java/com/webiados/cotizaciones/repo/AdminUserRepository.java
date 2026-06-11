package com.webiados.cotizaciones.repo;

import com.webiados.cotizaciones.domain.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
