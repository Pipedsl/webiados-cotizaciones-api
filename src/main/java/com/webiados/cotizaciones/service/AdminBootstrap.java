package com.webiados.cotizaciones.service;

import com.webiados.cotizaciones.config.AppProperties;
import com.webiados.cotizaciones.domain.AdminUser;
import com.webiados.cotizaciones.repo.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminUserRepository adminRepo;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public AdminBootstrap(AdminUserRepository adminRepo, PasswordEncoder passwordEncoder,
                          AppProperties props) {
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        String email = props.admin().bootstrapEmail();
        String password = props.admin().bootstrapPassword();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.info("ADMIN_BOOTSTRAP_EMAIL/PASSWORD no configurados, saltando bootstrap");
            return;
        }

        if (adminRepo.existsByEmailIgnoreCase(email)) {
            log.info("Admin '{}' ya existe, saltando bootstrap", email);
            return;
        }

        var admin = new AdminUser(UUID.randomUUID(), email, passwordEncoder.encode(password));
        adminRepo.save(admin);
        log.info("Admin '{}' creado exitosamente", email);
    }
}
