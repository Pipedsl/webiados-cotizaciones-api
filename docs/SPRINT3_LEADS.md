# Sprint 3 — Recibir los leads del outbound

> Diseño: 2026-08-03. **Contrato decidido por Felipe: 2026-08-04.** Backend implementado el
> 2026-08-04 (en rama, sin desplegar). Contexto: [`SIGUIENTE.md`](../SIGUIENTE.md) §4.

## DoD
Un lead que responde se convierte en `Quote` **sin retipear los datos**.

## Contrato (decidido)
1. **El formulario público de `webiados.com` postea al CORE**, no a este servicio. El CRM vive en
   el Core (`webiados/Demos-Webiados-Clientes`), ya funcionando en producción.
2. **Este servicio LEE** un lead del Core cuando el admin lo convierte en cotización. Usa los
   **campos del CRM del Core** (sin esquema paralelo).
3. **Se acepta el borrador sin opciones:** un lead todavía no tiene opciones ni precios; el
   vendedor los agrega después (con el catálogo del Core, §3).

## Cómo es el endpoint del Core (verificado en su repo)
- `GET https://core.webiados.com/api/v1/leads` → `{ docs, total, page, totalPages }`, filtros
  `estado` / `origen` / `limit`. **Auth:** `Authorization: Bearer wcore_live_<llave>` — el tenant
  sale de la llave. **No hay "traer por id"** (solo la lista), así que se busca en los recientes.
- Campos del lead: `id, nombre, email, telefono, mensaje, interes (json), origen, estado`.

## Lo que se implementó (backend, este repo)
- **Borradores:** `CreateQuoteRequest.options` dejó de ser `@NotEmpty`; `create` tolera sin
  opciones; `send` ya se niega a enviar una cotización sin opciones.
- **`LeadClient`:** lee `GET /api/v1/leads` con la llave (Bearer). No cachea (un lead es dato
  vivo). Si falta `CORE_API_KEY`, falla con un mensaje claro; el servicio arranca igual.
- **`GET /api/admin/leads`** (lista para el panel) y **`POST /api/admin/quotes/from-lead`**
  `{ leadId }` → crea el borrador (nombre normalizado, correo, y el contexto del lead —mensaje,
  teléfono, interés— en las notas internas).
- **Config:** `LEADS_URL`, `CORE_API_KEY`, `LEADS_TIMEOUT_SECONDS`.
- **Verificado:** pruebas con un Core simulado en proceso (parseo, header Bearer, find-by-id),
  más borrador PENDING sin opciones y la conversión mapeando los campos.

## Lo que falta
- 🚧 **NECESITA A FELIPE:** poner `CORE_API_KEY` (la llave de tenant del Core) en Railway. Sin
  ella la conversión no funciona en producción; el resto del servicio sí.
- **Frontend** (`webiados/webiados`): listar leads y el botón "convertir a cotización". Otro repo.
- ~~**Limitación:** el Core no expone lead por id; se busca entre los ~200 recientes.~~
  **Resuelto el 2026-08-05.** El Core ya expone `GET /api/v1/leads/:id` (misma llave Bearer; 404
  `problem+json` si no existe *o* es de otro tenant, sin distinguir los dos casos a propósito) y
  `LeadClient.find` lo usa. Se acabó el techo de 200 leads.
