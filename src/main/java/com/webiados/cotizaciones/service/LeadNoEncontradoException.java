package com.webiados.cotizaciones.service;

import java.util.NoSuchElementException;

/**
 * El Core no tiene ese lead para esta llave de tenant.
 *
 * <p>El Core responde 404 tanto si el lead no existe como si es de otro sitio, y no distingue los
 * dos casos a propósito: decir "existe, pero no es tuyo" le confirmaría a quien prueba números que
 * ESE lead está en otra parte. Por eso el mensaje tampoco los distingue.
 *
 * <p>Extiende {@link NoSuchElementException} para que {@code ApiExceptionHandler} lo traduzca a un
 * 404 con el mensaje entendible, en vez de un 500 genérico.
 */
public class LeadNoEncontradoException extends NoSuchElementException {

    private final long leadId;

    public LeadNoEncontradoException(long leadId) {
        super("El lead " + leadId + " no existe en el CRM del Core, o es de otro sitio. "
                + "Revisa el número en el CRM y vuelve a intentarlo.");
        this.leadId = leadId;
    }

    public long leadId() {
        return leadId;
    }
}
