package com.webiados.cotizaciones.service;

import com.webiados.cotizaciones.repo.QuoteRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CodeGenerator {

    private static final String CODIGO_CHARS = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final String CLAVE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int CODIGO_LENGTH = 10;
    private static final int CLAVE_LENGTH = 10;
    private static final int MAX_RETRIES = 20;

    private final SecureRandom rng = new SecureRandom();
    private final QuoteRepository quoteRepository;

    public CodeGenerator(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    /** Genera un codigo URL único (10 chars, min-ambiguos). */
    public String generateCodigo() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            String candidate = random(CODIGO_CHARS, CODIGO_LENGTH);
            if (!quoteRepository.existsByCodigo(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No se pudo generar un codigo único");
    }

    /** Genera una clave de acceso legible (10 chars). */
    public String generateClave() {
        return random(CLAVE_CHARS, CLAVE_LENGTH);
    }

    private String random(String chars, int length) {
        var sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
