package com.webiados.cotizaciones.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el Bearer token, valida la firma y coloca una Authentication con rol
 * ROLE_ADMIN o ROLE_CLIENT. Para tokens de cliente, expone el claim "codigo"
 * como detalle para que los controllers verifiquen que coincide con el path.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                String scope = claims.get(JwtService.CLAIM_SCOPE, String.class);
                if (scope != null) {
                    var authority = new SimpleGrantedAuthority("ROLE_" + scope.toUpperCase());
                    var principal = new JwtPrincipal(
                            claims.getSubject(),
                            scope,
                            claims.get(JwtService.CLAIM_CODIGO, String.class));
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(authority));
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // token inválido/expirado -> queda sin autenticar, responderá 401/403
            }
        }
        chain.doFilter(request, response);
    }

    public record JwtPrincipal(String subject, String scope, String codigo) {
    }
}
