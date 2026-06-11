package com.webiados.cotizaciones.service;

import com.webiados.cotizaciones.repo.AdminUserRepository;
import com.webiados.cotizaciones.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AdminUserRepository adminRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AdminUserRepository adminRepo, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** @return JWT admin o lanza IllegalArgumentException si credenciales inválidas. */
    public String adminLogin(String email, String password) {
        var user = adminRepo.findByEmailIgnoreCase(email)
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
        return jwtService.issueAdminToken(user.getEmail());
    }

    /** @return JWT cliente o lanza IllegalArgumentException si código/clave inválidos. */
    public String clientUnlock(String codigo, String clave, String claveHash) {
        if (!passwordEncoder.matches(clave, claveHash)) {
            throw new IllegalArgumentException("Código o clave incorrectos");
        }
        return jwtService.issueClientToken(codigo);
    }
}
