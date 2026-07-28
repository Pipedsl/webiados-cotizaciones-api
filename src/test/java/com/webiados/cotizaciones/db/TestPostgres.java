package com.webiados.cotizaciones.db;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Un Postgres real, embebido, compartido por toda la suite.
 *
 * <p>Las migraciones usan tipos y cláusulas propias de Postgres ({@code TIMESTAMPTZ},
 * {@code UUID}, {@code ON DELETE SET NULL}, {@code CHECK}). Probarlas contra H2 daría
 * verde sin probar lo que realmente corre en Railway. Zonky levanta un binario nativo de
 * Postgres 16, así que no hace falta Docker.
 *
 * <p>{@link #freshDatabase()} entrega una base vacía y exclusiva por test, para que el
 * estado de uno no contamine al siguiente.
 */
public final class TestPostgres {

    private static EmbeddedPostgres instance;
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private TestPostgres() {
    }

    private static synchronized EmbeddedPostgres instance() {
        if (instance == null) {
            try {
                instance = EmbeddedPostgres.builder().start();
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo levantar el Postgres embebido", e);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    instance.close();
                } catch (IOException ignored) {
                    // la JVM se está cerrando igual
                }
            }));
        }
        return instance;
    }

    /** Crea una base nueva y vacía, y devuelve un DataSource apuntando a ella. */
    public static DataSource freshDatabase() {
        EmbeddedPostgres pg = instance();
        String name = "test_" + COUNTER.incrementAndGet();

        try (Connection conn = pg.getPostgresDatabase().getConnection();
             Statement st = conn.createStatement()) {
            st.execute("CREATE DATABASE " + name);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la base " + name, e);
        }

        var ds = new PGSimpleDataSource();
        ds.setServerNames(new String[]{"localhost"});
        ds.setPortNumbers(new int[]{pg.getPort()});
        ds.setDatabaseName(name);
        ds.setUser("postgres");
        return ds;
    }
}
