package com.webiados.cotizaciones.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FormatosTest {

    @Nested
    @DisplayName("vigencia: UTC guardado, mostrado en horario de Chile, humano y sin hora")
    class Vigencia {

        @Test
        void formatea_en_espanol_dia_y_mes_sin_hora() {
            // 18-ago-2026 mediodía UTC → 08:00 en Santiago, sigue siendo el 18. Martes.
            assertThat(Formatos.vigencia(Instant.parse("2026-08-18T15:00:00Z")))
                    .isEqualTo("martes 18 de agosto");
        }

        @Test
        void convierte_de_UTC_a_Santiago_aunque_cambie_el_dia() {
            // 02:43 UTC del 18 → 22:43 del 17 en Santiago (UTC-4 en agosto). Debe decir 17, no 18.
            assertThat(Formatos.vigencia(Instant.parse("2026-08-18T02:43:01.902553Z")))
                    .isEqualTo("lunes 17 de agosto");
        }

        @Test
        void nunca_muestra_el_timestamp_crudo() {
            assertThat(Formatos.vigencia(Instant.parse("2026-08-18T02:43:01.902553Z")))
                    .doesNotContain("T", "Z", ":", "2026");
        }
    }

    @Nested
    @DisplayName("nombre: se normaliza para que salga bien en correo, landing y panel")
    class Nombre {

        @Test
        void capitaliza_un_nombre_en_minuscula() {
            assertThat(Formatos.nombre("felipe")).isEqualTo("Felipe");
        }

        @Test
        void baja_mayusculas_de_mas() {
            assertThat(Formatos.nombre("MACARENA LARRAÍN")).isEqualTo("Macarena Larraín");
        }

        @Test
        void deja_las_particulas_en_minuscula_menos_al_inicio() {
            assertThat(Formatos.nombre("pastelería vientos del sur"))
                    .isEqualTo("Pastelería Vientos del Sur");
        }

        @Test
        void recorta_y_colapsa_espacios() {
            assertThat(Formatos.nombre("  felipe   navarrete  ")).isEqualTo("Felipe Navarrete");
        }

        @Test
        void tolera_null_y_vacio() {
            assertThat(Formatos.nombre(null)).isNull();
            assertThat(Formatos.nombre("   ")).isEmpty();
        }
    }
}
