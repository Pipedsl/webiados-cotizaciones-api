# Diseño — Panel de leads + selector de catálogo

**Fecha:** 2026-08-07
**Repos afectados:** `webiados-cotizaciones-api` (backend) y `webiados/webiados` (frontend Angular)
**Objetivo:** cerrar el ciclo **lead → borrador → opciones con precios del Core**, sin escribir
precios a mano ni Markdown. Aprovecha lo ya vivo en producción: §3 (`GET /api/admin/pricing`) y
§4 (`GET /api/admin/leads`, `POST /api/admin/quotes/from-lead`), con `CORE_API_KEY` ya configurada
y confirmada funcionando.

---

## 1. Flujo objetivo

```
Panel → pestaña "Leads" → filtro por estado del CRM → [Convertir a borrador]
   → aterriza en el detalle del borrador nuevo (nace SIN opciones)
   → [Agregar opción] con selector de catálogo (precarga precio del Core)
   → repetir por cada opción → [Enviar]
```

Decisiones de alcance (YAGNI):
- **No** se rastrea qué leads ya se convirtieron: el backend no modela ese vínculo. La lista los
  muestra todos según su estado en el CRM.
- La lista trae un `limit` razonable (≈20). Sin paginación en v1.
- El selector **precarga** valores y los deja **editables**; no bloquea nada.

---

## 2. Tramo 1 — Backend (`cotizaciones-api`)

Hoy el read-through expone solo `landings`, `kits`, `addons`. El Core publica además `identidad`,
`piezas`, `horas`. Se amplía el DTO para exponer las seis.

### Cambios
- `dto/pricing/PricingCatalog.java`: sumar
  - `List<ItemPrecio> identidad` (misma forma que landings/kits: `nombre`, `setup`, `mensual`, `*Monto`),
  - `List<ItemPrecioSimple> piezas`,
  - `List<ItemPrecioSimple> horas`.
- `dto/pricing/ItemPrecioSimple.java` (**nuevo**): `record ItemPrecioSimple(String nombre,
  BigDecimal precio, Monto precioMonto)` con `@JsonIgnoreProperties(ignoreUnknown = true)`.
  `piezas`/`horas` traen un `precio` suelto (pago único), sin setup/mensual.
- No cambia `PricingClient` ni `PricingController`: el read-through deserializa en `PricingCatalog`
  y lo pasa verbatim. Al sumar campos mapeados, se poblan solos.

### Verificación
- `PricingClientTest`: extender el stub del Core (in-process, nunca producción) para incluir las tres
  categorías nuevas y afirmar que llegan con sus montos. `piezas`/`horas` con `precio`/`precioMonto`.
- Suite completa verde.
- Commit → **redeploy a Railway con `railway up`, con OK explícito de Felipe** (es aditivo).
- Verificar en prod: `GET core.webiados.com/api/v1/pricing` ya trae las seis; tras el deploy,
  `GET /api/admin/pricing` (con token) debe reflejarlas. Como no tomo el token de admin, la
  verificación de punta a punta de este endpoint la corre Felipe o se valida vía el frontend.

---

## 3. Tramo 2 — Frontend (`webiados/webiados`, en una rama)

El panel es un componente `Admin` que alterna vistas con una señal (`login` | `dashboard` |
`nueva` | `detalle`), no con el router. Se suma la vista `leads` al mismo patrón.

### 3.1 Modelos (`shared/cotizaciones/cotizaciones.model.ts`)
- `Lead`: `{ id: number; nombre: string; email: string | null; telefono: string | null;
  mensaje: string | null; origen: string | null; estado: string; interes: unknown }`
  (verificado contra `dto/lead/Lead.java`: `id` es `Long` **numérico**; `interes` es JSON libre del CRM).
- `LeadPage`: `{ docs: Lead[]; total: number; page: number; totalPages: number }`.
  Confirmado: `/api/admin/leads` **siempre** devuelve esta forma (`LeadController` → `LeadPage`),
  no una lista plana. La vista lee `docs`.
- `PricingCatalog` + `PricingItem`: espejo de las seis categorías. `PricingItem` normalizado para el
  selector: `{ categoria, label, precioNeto, precioConIva, mensualNeto | null, mensualConIva | null }`.
  La normalización vive en el frontend (landings/kits/identidad/addons → setup+mensual;
  piezas/horas → precio suelto, mensual null).

### 3.2 API (`shared/cotizaciones/quotes-api.ts`)
- `listLeads(estado?: string, limit = 20, token)` → `GET /api/admin/leads?estado=&limit=` → `LeadPage`
  (el backend por defecto trae 50; la UI pide ≈20 explícito). La vista usa `page.docs`.
- `convertLead(leadId: number, token)` → `POST /api/admin/quotes/from-lead` con `{ leadId }`
  (numérico) → `QuoteAdminDetail`.
- `getPricing(token)` → `GET /api/admin/pricing` → `PricingCatalog`.
- `addOption(quoteId, body: CreateOptionRequest, token)` → `POST /api/admin/quotes/{id}/options`.
  **Hoy no existe en el front** y es imprescindible: un borrador desde lead nace sin opciones.

### 3.3 Vista `leads/` (nueva sección)
- `leads.ts` / `leads.html` / `leads.css`, en `pages/admin/sections/leads/`.
- Filtro por estado (`nuevo` · `contactado` · `calificado` · `propuesta` · `ganado` · `perdido`),
  con opción "Todos". Estados tomados del CRM del Core.
- Cada fila: nombre + interés (rubro/servicio si vienen), origen, estado, y botón **Convertir**.
- Al convertir con éxito → emite evento con el `id` del borrador; `Admin` cambia a `detalle`.
- Enganche en `admin.ts`: sumar `'leads'` a `AdminView`, importar la sección, y un tab en la
  cabecera del panel junto a "Cotizaciones".

### 3.4 `detalle` — Agregar opción (pieza faltante) + selector
- Construir el flujo **Agregar opción** (POST), que hoy no existe: formulario igual al de editar,
  llamando `addOption`. Tras crear, recargar el detalle o insertar la opción en la señal.
- Selector de catálogo en el formulario de agregar **y** de editar opción.

### 3.5 `nueva` — selector en el formulario de opción
- Mismo selector, precargando `titulo`, `precio` (neto) y `precioMensual` (neto).

### 3.6 `PricingStore` compartido
- Servicio inyectable que trae el catálogo una vez por sesión de panel y lo cachea en una señal.
  Lo usan `nueva` y `detalle`. Evita pedir el catálogo en cada formulario.

### 3.7 Selector de catálogo (comportamiento)
- Agrupa por **Landings · Kits · Addons · Identidad · Piezas · Horas**.
- Al elegir un ítem precarga:
  - landings/kits/identidad/addons → `precio` = `setup` neto, `precioMensual` = `mensual` neto
    (null si 0), `titulo` = `nombre` (o `etiqueta` en addons).
  - piezas/horas → `precio` = `precio` neto, `precioMensual` = null, `titulo` = `nombre`.
- Junto a cada ítem, el total **con IVA** como referencia (el número que ve el cliente).
- **Todo editable** tras precargar.
- **Fallback:** si `getPricing` falla (Core caído), el selector muestra "catálogo no disponible,
  ingresa el precio a mano" y los campos manuales siguen funcionando. Nunca se bloquea cotizar
  porque el Core esté abajo — mismo espíritu que el cache-fallback del backend.

---

## 4. Errores y estados (frontend)

- **Lista de leads:** loading / vacío ("no hay leads en estado X") / error. El mensaje de error del
  backend (incluido el que menciona `CORE_API_KEY`) se muestra tal cual. `401`/`403` → cerrar sesión.
- **Convertir:** spinner en el botón; en error, mensaje; `401` → cerrar sesión.
- **Catálogo:** se pide una vez; si falla, fallback a mano (ver 3.7). No es error bloqueante.

---

## 5. Convenciones y entrega

- **Español de Chile, sin voseo**, en todo texto nuevo de UI y mensajes.
- **Guía obligatoria** en `docs/guias/` de **cada** repo tocado, en el **mismo commit** que el código:
  - backend: actualizar `cotizaciones-precios-del-core.md` (ahora seis categorías).
  - frontend: guía nueva del flujo lead → borrador → opciones en el panel.
- **Tests:**
  - backend: `PricingClientTest` con las seis categorías.
  - frontend: unitario del mapeo de precios (neto→precio; mensual 0→null; piezas/horas→sin mensual)
    y un e2e de "listar leads → convertir → aterrizar en detalle" (proporcional a la suite actual).
- **Rama en el frontend, no `main`:** la UI se verifica contra el backend en prod (ya vivo) antes de
  mergear. Felipe mergea → Vercel auto-despliega. El backend se despliega a Railway a mano, con su OK.

---

## 6. Orden de implementación

1. Backend: DTO + `ItemPrecioSimple` + tests → commit + guía → deploy (con OK).
2. Frontend: modelos + API (incl. `addOption`) → `PricingStore` → vista `leads` → detalle
   (agregar opción + selector) → nueva (selector) → tests + guía → rama lista para merge.

Cada tramo se puede verificar por separado: el backend contra su stub y prod; el frontend contra el
backend ya desplegado.
