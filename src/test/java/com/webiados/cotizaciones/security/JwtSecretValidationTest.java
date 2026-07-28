package com.webiados.cotizaciones.security;

import com.webiados.cotizaciones.config.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Un secreto no tiene default: si falta, el servicio debe negarse a arrancar. Como
 * JwtService se construye al levantar el contexto, que su constructor lance equivale a que
 * el arranque falle.
 */
class JwtSecretValidationTest {

    private static JwtService construir(String secret) {
        return new JwtService(new JwtProperties(secret, 480, 30));
    }

    @Test
    @DisplayName("sin secreto (null) no arranca")
    void nullNoArranca() {
        assertThatThrownBy(() -> construir(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET no está definido");
    }

    @Test
    @DisplayName("secreto en blanco no arranca")
    void blancoNoArranca() {
        assertThatThrownBy(() -> construir("   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET no está definido");
    }

    @Test
    @DisplayName("el placeholder sin resolver (JWT_SECRET ausente) se trata como no definido")
    void placeholderSinResolverNoArranca() {
        assertThatThrownBy(() -> construir("${JWT_SECRET}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET no está definido");
    }

    @Test
    @DisplayName("el secreto de desarrollo que quedó en git se rechaza explícitamente")
    void secretoComprometidoNoArranca() {
        assertThatThrownBy(() ->
                construir("change-me-in-prod-this-is-a-very-long-dev-secret-key-0123456789"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("público");
    }

    @Test
    @DisplayName("un secreto demasiado corto no arranca")
    void cortoNoArranca() {
        assertThatThrownBy(() -> construir("corto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demasiado corto");
    }

    @Test
    @DisplayName("un secreto válido arranca y firma un token de admin")
    void secretoValidoArranca() {
        var secret = "un-secreto-de-verdad-largo-y-aleatorio-para-el-test-0123456789ab";
        assertThatCode(() -> {
            var jwt = construir(secret);
            var token = jwt.issueAdminToken("admin@webiados.com");
            assertThat(jwt.parse(token).get("scope")).isEqualTo("admin");
        }).doesNotThrowAnyException();
    }
}
