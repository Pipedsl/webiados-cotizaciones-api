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

    /** Mínimo para HS256. Por debajo de esto un secreto no es criptográficamente serio. */
    private static final int MIN_SECRET_BYTES = 32;

    /**
     * El default que vivió en application.yml y quedó en el historial de git. Es público:
     * cualquiera que lo use estaría firmando tokens con un secreto conocido, así que se
     * rechaza explícitamente aunque alguien lo ponga a mano en JWT_SECRET.
     */
    private static final String COMPROMISED_DEV_SECRET =
            "change-me-in-prod-this-is-a-very-long-dev-secret-key-0123456789";

    private final SecretKey key;
    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = buildKey(props.secret());
    }

    /**
     * Valida el secreto al construir el bean, es decir, en el arranque. Si algo está mal,
     * el contexto de Spring no levanta y el servicio no queda expuesto: falla y grita.
     */
    private static SecretKey buildKey(String secret) {
        // Sin default en application.yml, un JWT_SECRET ausente deja el placeholder literal
        // "${JWT_SECRET}" sin resolver. Se trata igual que ausente.
        if (secret == null || secret.isBlank() || secret.startsWith("${")) {
            throw new IllegalStateException(
                    "JWT_SECRET no está definido. El servicio no arranca sin secreto: "
                            + "configura la variable de entorno JWT_SECRET con una cadena "
                            + "aleatoria de al menos 64 caracteres.");
        }
        if (secret.equals(COMPROMISED_DEV_SECRET)) {
            throw new IllegalStateException(
                    "JWT_SECRET tiene el valor de desarrollo que estuvo en el repositorio. "
                            + "Es público: genera un secreto nuevo y aleatorio.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET es demasiado corto (" + bytes.length + " bytes). "
                            + "Debe tener al menos " + MIN_SECRET_BYTES + " bytes (recomendado 64+).");
        }
        return Keys.hmacShaKeyFor(bytes);
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
