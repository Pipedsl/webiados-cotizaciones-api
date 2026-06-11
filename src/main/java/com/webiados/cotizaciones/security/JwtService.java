package com.webiados.cotizaciones.security;

import com.webiados.cotizaciones.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    public static final String SCOPE_ADMIN = "admin";
    public static final String SCOPE_CLIENT = "client";
    public static final String CLAIM_SCOPE = "scope";
    public static final String CLAIM_CODIGO = "codigo";

    private final SecretKey key;
    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAdminToken(String subject) {
        return build(subject, SCOPE_ADMIN, null, props.adminTtlMinutes());
    }

    public String issueClientToken(String codigo) {
        return build(codigo, SCOPE_CLIENT, codigo, props.clientTtlMinutes());
    }

    private String build(String subject, String scope, String codigo, long ttlMinutes) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subject)
                .claim(CLAIM_SCOPE, scope)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttlMinutes, ChronoUnit.MINUTES)))
                .signWith(key);
        if (codigo != null) {
            builder.claim(CLAIM_CODIGO, codigo);
        }
        return builder.compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
