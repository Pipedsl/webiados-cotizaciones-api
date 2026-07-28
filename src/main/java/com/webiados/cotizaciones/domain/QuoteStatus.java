package com.webiados.cotizaciones.domain;

/**
 * Estado de una cotización en el embudo comercial.
 *
 * <p>{@link #PENDING}, {@link #SENT}, {@link #SELECTED} y {@link #REJECTED} se
 * <strong>persisten</strong> (columna {@code quote.status}): son hechos que alguien
 * decidió y que hay que poder contar.
 *
 * <p>{@link #EXPIRED} <strong>no se persiste</strong>: es función del reloj, no de una
 * decisión. Se deriva en {@link Quote#statusAt(java.time.Instant)} sobre las cotizaciones
 * que quedaron en {@code PENDING} o {@code SENT}.
 */
public enum QuoteStatus {

    /** Creada en el sistema, todavía no enviada al cliente. Borrador. */
    PENDING,

    /** Enviada al cliente. Tiene {@code sent_at}. Es el denominador de la tasa de cierre. */
    SENT,

    /** El cliente eligió una opción. Tiene {@code selected_at}. Equivale a "aceptada". */
    SELECTED,

    /** El cliente dijo que no. Tiene {@code rejected_at}. */
    REJECTED,

    /** Derivado: venció sin que el cliente respondiera. Nunca se guarda. */
    EXPIRED
}
