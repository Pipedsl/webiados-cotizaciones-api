package com.webiados.cotizaciones.service;

import com.webiados.cotizaciones.dto.admin.CreateQuoteRequest;
import com.webiados.cotizaciones.dto.admin.CreateQuoteResponse;
import com.webiados.cotizaciones.dto.admin.QuoteAdminDetail;
import com.webiados.cotizaciones.dto.lead.Lead;
import com.webiados.cotizaciones.dto.lead.LeadPage;
import org.springframework.stereotype.Service;

/**
 * Convierte un lead del CRM del Core en un <strong>borrador</strong> de cotización, sin retipear
 * los datos. El borrador nace en {@code PENDING} y sin opciones: el vendedor les pone precio
 * después, con el catálogo del Core. El contexto del lead (mensaje, interés, teléfono) queda en
 * las notas internas para que el vendedor lo tenga a la vista.
 */
@Service
public class LeadService {

    private final LeadClient leads;
    private final QuoteService quotes;

    public LeadService(LeadClient leads, QuoteService quotes) {
        this.leads = leads;
        this.quotes = quotes;
    }

    public LeadPage listar(String estado, int limit) {
        return leads.list(estado, limit);
    }

    public QuoteAdminDetail convertirABorrador(long leadId) {
        Lead lead = leads.find(leadId);
        var req = new CreateQuoteRequest(
                lead.nombre(),          // se normaliza al guardar (Formatos.nombre)
                lead.email(),
                notasDe(lead),
                null, null, null, null,
                null                    // sin opciones → borrador
        );
        CreateQuoteResponse creada = quotes.create(req);
        return quotes.getDetail(creada.id());
    }

    /** Junta el contexto del lead en las notas internas del borrador. */
    private String notasDe(Lead lead) {
        StringBuilder sb = new StringBuilder("Lead del CRM del Core");
        if (lead.origen() != null) {
            sb.append(" (origen: ").append(lead.origen()).append(')');
        }
        sb.append('.');
        if (lead.telefono() != null && !lead.telefono().isBlank()) {
            sb.append(" Teléfono: ").append(lead.telefono()).append('.');
        }
        if (lead.mensaje() != null && !lead.mensaje().isBlank()) {
            sb.append(" Mensaje: \"").append(lead.mensaje().trim()).append("\".");
        }
        if (lead.interes() != null && !lead.interes().isNull()) {
            sb.append(" Interés: ").append(lead.interes().toString()).append('.');
        }
        return sb.toString();
    }
}
