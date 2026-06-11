package com.webiados.cotizaciones.web;

import com.webiados.cotizaciones.dto.admin.LoginRequest;
import com.webiados.cotizaciones.dto.admin.TokenResponse;
import com.webiados.cotizaciones.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.adminLogin(req.email(), req.password());
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
