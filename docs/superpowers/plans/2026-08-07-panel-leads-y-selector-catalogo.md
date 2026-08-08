# Panel de leads + selector de catálogo — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cerrar el ciclo lead → borrador → opciones con precios del Core, sin escribir precios a mano.

**Architecture:** Dos tramos secuenciales. Tramo 1 amplía el read-through del catálogo del backend
(`cotizaciones-api`) a las 6 categorías del Core. Tramo 2 construye el flujo en el panel Angular
(`webiados/webiados`): vista de leads, conversión a borrador, y un selector de catálogo que precarga
precios al agregar/editar opciones. El frontend consume los endpoints ya vivos en producción.

**Tech Stack:** Backend: Spring Boot 3 / Java 21, records, Jackson. Frontend: Angular (standalone
components, signals, `ChangeDetectionStrategy.OnPush`, ReactiveForms), tests con **vitest** (unit) y
**Playwright** (e2e).

## Global Constraints

- **Precios verbatim del Core:** nunca hardcodear ni convertir montos. Se pasan tal cual (`neto`,
  `conIva`, `uf`, `usd`).
- **Español de Chile, sin voseo** en todo texto de UI y mensajes (`tienes`, `revisa`, `mira`, `agrégalo`).
- **Guía obligatoria** en `docs/guias/` del repo tocado, en el **mismo commit** que el código.
- **Backend:** Java 21, records, `@JsonIgnoreProperties(ignoreUnknown = true)` en todo DTO del Core.
- **Frontend:** standalone components, `inject()`, signals, `OnPush`, ReactiveForms — como el resto del panel.
- **Frontend en rama** (no `main`). **Backend deploy manual** a Railway con `railway up`, con OK explícito de Felipe.
- Endpoints ya vivos en prod: `GET /api/admin/pricing`, `GET /api/admin/leads`, `POST /api/admin/quotes/from-lead`.
- Base URL prod: `https://cotizaciones-api-production-e0fb.up.railway.app`.

---

# TRAMO 1 — Backend (`webiados-cotizaciones-api`)

### Task 1: Exponer las 6 categorías del catálogo

**Files:**
- Create: `src/main/java/com/webiados/cotizaciones/dto/pricing/ItemPrecioSimple.java`
- Modify: `src/main/java/com/webiados/cotizaciones/dto/pricing/PricingCatalog.java`
- Test: `src/test/java/com/webiados/cotizaciones/service/PricingClientTest.java`

**Interfaces:**
- Consumes: `Monto` (existente), `ItemPrecio` (existente), `PricingClient(String url, RestClient http)`.
- Produces: `PricingCatalog` con `List<ItemPrecio> identidad`, `List<ItemPrecioSimple> piezas`,
  `List<ItemPrecioSimple> horas`. `ItemPrecioSimple(String nombre, BigDecimal precio, Monto precioMonto)`.

- [ ] **Step 1: Ampliar el catálogo de prueba en `PricingClientTest`**

En la constante `CATALOGO`, agregar las tres categorías nuevas (dentro del mismo JSON, junto a `addons`):

```json
"identidad":[
  {"nombre":"Logo","setup":180000,"mensual":0,
   "setupMonto":{"uf":4.4069,"neto":180000,"conIva":214200,"usd":234},
   "mensualMonto":{"uf":0,"neto":0,"conIva":0,"usd":0},
   "primerAnioMonto":{"uf":4.4069,"neto":180000,"conIva":214200,"usd":234}}
],
"piezas":[
  {"nombre":"Set de íconos personalizados (hasta 8)","precio":120000,
   "precioMonto":{"uf":2.938,"neto":120000,"conIva":142800,"usd":156}}
],
"horas":[
  {"nombre":"Hora de desarrollo","precio":35000,
   "precioMonto":{"uf":0.8569,"neto":35000,"conIva":41650,"usd":46}}
]
```

- [ ] **Step 2: Escribir el test que falla**

Agregar este test a `PricingClientTest`:

```java
@Test
void expone_identidad_piezas_y_horas() throws IOException {
    HttpServer core = startCore(CATALOGO);
    try {
        var client = new PricingClient(urlOf(core), RestClient.create());

        PricingCatalog cat = client.get();

        // identidad reusa ItemPrecio (nombre + setup)
        assertThat(cat.identidad()).hasSize(1);
        assertThat(cat.identidad().get(0).nombre()).isEqualTo("Logo");
        assertThat(cat.identidad().get(0).setupMonto().conIva()).isEqualByComparingTo("214200");

        // piezas/horas: precio suelto, sin setup/mensual
        assertThat(cat.piezas()).hasSize(1);
        assertThat(cat.piezas().get(0).nombre()).isEqualTo("Set de íconos personalizados (hasta 8)");
        assertThat(cat.piezas().get(0).precio()).isEqualByComparingTo("120000");
        assertThat(cat.piezas().get(0).precioMonto().conIva()).isEqualByComparingTo("142800");

        assertThat(cat.horas()).hasSize(1);
        assertThat(cat.horas().get(0).precioMonto().neto()).isEqualByComparingTo("35000");
    } finally {
        core.stop(0);
    }
}
```

- [ ] **Step 3: Correr el test y verlo fallar**

Run: `./mvnw -q -Dtest=PricingClientTest test`
Expected: FAIL — `PricingCatalog` no tiene los métodos `identidad()`/`piezas()`/`horas()` (no compila).

- [ ] **Step 4: Crear `ItemPrecioSimple`**

```java
package com.webiados.cotizaciones.dto.pricing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Un ítem del catálogo con <strong>precio suelto</strong> (pago único), sin setup/mensual. Es la
 * forma de {@code piezas} y {@code horas} en el Core: {@code { nombre, precio, precioMonto }}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemPrecioSimple(String nombre, BigDecimal precio, Monto precioMonto) {
}
```

- [ ] **Step 5: Sumar los campos a `PricingCatalog`**

Agregar al record (después de `addons`):

```java
        List<ItemPrecio> addons,
        List<ItemPrecio> identidad,
        List<ItemPrecioSimple> piezas,
        List<ItemPrecioSimple> horas
```

(Ajustar la lista de parámetros del record y mantener el orden; `identidad` va con `ItemPrecio`.)

- [ ] **Step 6: Correr el test y verlo pasar**

Run: `./mvnw -q -Dtest=PricingClientTest test`
Expected: PASS.

- [ ] **Step 7: Correr la suite completa**

Run: `./mvnw -q test`
Expected: PASS (todo verde; no se rompió nada existente).

- [ ] **Step 8: Actualizar la guía**

En `docs/guias/cotizaciones-precios-del-core.md`, en la sección de qué trae el catálogo, cambiar
"tres categorías (landings, kits, addons)" por las **seis** (landings, kits, addons, identidad,
piezas, horas), aclarando que piezas/horas son de precio suelto.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/webiados/cotizaciones/dto/pricing/ItemPrecioSimple.java \
        src/main/java/com/webiados/cotizaciones/dto/pricing/PricingCatalog.java \
        src/test/java/com/webiados/cotizaciones/service/PricingClientTest.java \
        docs/guias/cotizaciones-precios-del-core.md
git commit -m "feat(precios): exponer identidad, piezas y horas del catálogo del Core"
```

- [ ] **Step 10: Deploy (requiere OK de Felipe)**

Pedir a Felipe el OK. Con el OK: `railway up --detach`, esperar `SUCCESS`, verificar
`GET /actuator/health` = `200 UP`. La verificación de `/api/admin/pricing` con token la corre Felipe
o se valida vía el frontend (no se toma el token de admin en la sesión).

---

# TRAMO 2 — Frontend (`webiados/webiados`, en una rama)

**Preparación (antes de la Task 2):** crear la rama de trabajo.

```bash
cd /Users/felipenavarretenavarrete/Desktop/Developer/webiados
git checkout -b feat/panel-leads-y-catalogo
```

### Task 2: Modelos + normalización del catálogo

**Files:**
- Modify: `src/app/shared/cotizaciones/cotizaciones.model.ts`
- Create: `src/app/shared/cotizaciones/catalogo.ts` (normalización)
- Test: `src/app/shared/cotizaciones/catalogo.spec.ts`

**Interfaces:**
- Produces: tipos `Lead`, `LeadPage`, `PricingCatalog`, `MontoRaw`, `CatalogItemRaw`,
  `CatalogSimpleRaw`, `PricingItem`, `CatalogCategoria`; función
  `normalizarCatalogo(cat: PricingCatalog): PricingItem[]`.

- [ ] **Step 1: Agregar los tipos a `cotizaciones.model.ts`**

```typescript
// --- Leads (CRM del Core, vía /api/admin/leads) ---
export interface Lead {
  readonly id: number;               // Long en el backend
  readonly nombre: string;
  readonly email: string | null;
  readonly telefono: string | null;
  readonly mensaje: string | null;
  readonly origen: string | null;
  readonly estado: string;
  readonly interes: unknown;         // JSON libre del CRM
}

export interface LeadPage {
  readonly docs: readonly Lead[];
  readonly total: number;
  readonly page: number;
  readonly totalPages: number;
}

// --- Catálogo del Core (vía /api/admin/pricing) ---
export interface MontoRaw {
  readonly uf: number; readonly neto: number; readonly conIva: number; readonly usd: number;
}
// landings / kits / identidad / addons
export interface CatalogItemRaw {
  readonly nombre?: string | null;
  readonly etiqueta?: string | null;
  readonly slug?: string | null;
  readonly setup: number;
  readonly mensual: number;
  readonly setupMonto: MontoRaw;
  readonly mensualMonto: MontoRaw;
  readonly primerAnioMonto?: MontoRaw | null;
}
// piezas / horas (precio suelto)
export interface CatalogSimpleRaw {
  readonly nombre: string;
  readonly precio: number;
  readonly precioMonto: MontoRaw;
}
export interface PricingCatalog {
  readonly moneda: string;
  readonly iva: number;
  readonly landings: readonly CatalogItemRaw[];
  readonly kits: readonly CatalogItemRaw[];
  readonly addons: readonly CatalogItemRaw[];
  readonly identidad: readonly CatalogItemRaw[];
  readonly piezas: readonly CatalogSimpleRaw[];
  readonly horas: readonly CatalogSimpleRaw[];
}

export type CatalogCategoria =
  | 'Landings' | 'Kits' | 'Addons' | 'Identidad' | 'Piezas' | 'Horas';

// Ítem del catálogo aplanado para el selector. precio*/mensual* en NETO (lo que guarda el form);
// el *ConIva es solo referencia visual.
export interface PricingItem {
  readonly categoria: CatalogCategoria;
  readonly label: string;
  readonly precioNeto: number;
  readonly precioConIva: number;
  readonly mensualNeto: number | null;
  readonly mensualConIva: number | null;
}
```

- [ ] **Step 2: Escribir el test que falla en `catalogo.spec.ts`**

```typescript
import { describe, it, expect } from 'vitest';
import { normalizarCatalogo } from './catalogo';
import type { PricingCatalog } from './cotizaciones.model';

const monto = (neto: number, conIva: number) => ({ uf: 0, neto, conIva, usd: 0 });

const CAT: PricingCatalog = {
  moneda: 'CLP', iva: 0.19,
  landings: [],
  kits: [{
    nombre: 'Vitrina', setup: 390000, mensual: 29000,
    setupMonto: monto(390000, 464100), mensualMonto: monto(29000, 34510),
    primerAnioMonto: monto(738000, 878220),
  }],
  addons: [{
    etiqueta: 'Sección extra', slug: 'seccion_extra', setup: 60000, mensual: 0,
    setupMonto: monto(60000, 71400), mensualMonto: monto(0, 0),
  }],
  identidad: [],
  piezas: [{ nombre: 'Set de íconos', precio: 120000, precioMonto: monto(120000, 142800) }],
  horas: [{ nombre: 'Hora de desarrollo', precio: 35000, precioMonto: monto(35000, 41650) }],
};

describe('normalizarCatalogo', () => {
  const items = normalizarCatalogo(CAT);

  it('mapea un kit: setup neto → precio, mensual neto, con IVA de referencia', () => {
    const vitrina = items.find((i) => i.label === 'Vitrina')!;
    expect(vitrina.categoria).toBe('Kits');
    expect(vitrina.precioNeto).toBe(390000);
    expect(vitrina.precioConIva).toBe(464100);
    expect(vitrina.mensualNeto).toBe(29000);
    expect(vitrina.mensualConIva).toBe(34510);
  });

  it('un addon con mensual 0 deja mensualNeto en null', () => {
    const extra = items.find((i) => i.label === 'Sección extra')!;
    expect(extra.categoria).toBe('Addons');
    expect(extra.precioNeto).toBe(60000);
    expect(extra.mensualNeto).toBeNull();
  });

  it('piezas y horas usan precio suelto y no tienen mensual', () => {
    const pieza = items.find((i) => i.categoria === 'Piezas')!;
    expect(pieza.precioNeto).toBe(120000);
    expect(pieza.mensualNeto).toBeNull();
    const hora = items.find((i) => i.categoria === 'Horas')!;
    expect(hora.precioNeto).toBe(35000);
    expect(hora.mensualNeto).toBeNull();
  });
});
```

- [ ] **Step 3: Correr el test y verlo fallar**

Run: `npm test -- --run catalogo`
Expected: FAIL — `normalizarCatalogo` no existe.

- [ ] **Step 4: Implementar `catalogo.ts`**

```typescript
import type {
  CatalogItemRaw,
  CatalogSimpleRaw,
  PricingCatalog,
  PricingItem,
  CatalogCategoria,
} from './cotizaciones.model';

const nuloSiCero = (n: number): number | null => (n && n > 0 ? n : null);

function deItem(raw: CatalogItemRaw, categoria: CatalogCategoria): PricingItem {
  return {
    categoria,
    label: (raw.nombre ?? raw.etiqueta ?? '').trim(),
    precioNeto: raw.setupMonto.neto,
    precioConIva: raw.setupMonto.conIva,
    mensualNeto: nuloSiCero(raw.mensualMonto.neto),
    mensualConIva: nuloSiCero(raw.mensualMonto.conIva),
  };
}

function deSimple(raw: CatalogSimpleRaw, categoria: CatalogCategoria): PricingItem {
  return {
    categoria,
    label: raw.nombre.trim(),
    precioNeto: raw.precioMonto.neto,
    precioConIva: raw.precioMonto.conIva,
    mensualNeto: null,
    mensualConIva: null,
  };
}

/** Aplana las 6 categorías del catálogo en una lista para el selector. */
export function normalizarCatalogo(cat: PricingCatalog): PricingItem[] {
  return [
    ...cat.landings.map((i) => deItem(i, 'Landings')),
    ...cat.kits.map((i) => deItem(i, 'Kits')),
    ...cat.addons.map((i) => deItem(i, 'Addons')),
    ...cat.identidad.map((i) => deItem(i, 'Identidad')),
    ...cat.piezas.map((i) => deSimple(i, 'Piezas')),
    ...cat.horas.map((i) => deSimple(i, 'Horas')),
  ];
}
```

- [ ] **Step 5: Correr el test y verlo pasar**

Run: `npm test -- --run catalogo`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/app/shared/cotizaciones/cotizaciones.model.ts \
        src/app/shared/cotizaciones/catalogo.ts \
        src/app/shared/cotizaciones/catalogo.spec.ts
git commit -m "feat(cotizaciones): modelos de lead + catálogo y normalización para el selector"
```

### Task 3: Métodos de API (leads, pricing, addOption)

**Files:**
- Modify: `src/app/shared/cotizaciones/quotes-api.ts`
- Modify: `src/app/shared/cotizaciones/cotizaciones.model.ts` (import de tipos nuevos)

**Interfaces:**
- Consumes: `Lead`, `LeadPage`, `PricingCatalog`, `CreateOptionRequest`, `QuoteAdminDetail`.
- Produces en `QuotesApi`: `listLeads(estado, limit, token)`, `convertLead(leadId, token)`,
  `getPricing(token)`, `addOption(quoteId, body, token)`.

- [ ] **Step 1: Agregar los imports de tipos**

En `quotes-api.ts`, sumar al `import type { … }`: `LeadPage`, `PricingCatalog`, `CreateOptionRequest`.

- [ ] **Step 2: Agregar los métodos a `QuotesApi`**

```typescript
  // --- Leads (CRM del Core) ---

  listLeads(estado: string | null, limit: number, token: string): Observable<LeadPage> {
    let url = `${API_BASE}/api/admin/leads?limit=${limit}`;
    if (estado) url += `&estado=${encodeURIComponent(estado)}`;
    return this.http.get<LeadPage>(url, { headers: this.bearer(token) });
  }

  // Convierte un lead en borrador de cotización (PENDING, sin opciones).
  convertLead(leadId: number, token: string): Observable<QuoteAdminDetail> {
    return this.http.post<QuoteAdminDetail>(
      `${API_BASE}/api/admin/quotes/from-lead`,
      { leadId },
      { headers: this.bearer(token) },
    );
  }

  // --- Catálogo de precios del Core (read-through del backend) ---

  getPricing(token: string): Observable<PricingCatalog> {
    return this.http.get<PricingCatalog>(`${API_BASE}/api/admin/pricing`, {
      headers: this.bearer(token),
    });
  }

  // Agrega una opción a una cotización existente (la pieza que faltaba para los borradores).
  addOption(
    quoteId: string,
    body: CreateOptionRequest,
    token: string,
  ): Observable<QuoteAdminDetail> {
    return this.http.post<QuoteAdminDetail>(
      `${API_BASE}/api/admin/quotes/${quoteId}/options`,
      body,
      { headers: this.bearer(token) },
    );
  }
```

- [ ] **Step 3: Verificar que compila**

Run: `npm run build`
Expected: build OK (sin errores de tipos).

- [ ] **Step 4: Commit**

```bash
git add src/app/shared/cotizaciones/quotes-api.ts
git commit -m "feat(cotizaciones): API de leads, catálogo y agregar opción"
```

### Task 4: `PricingStore` (catálogo cacheado por sesión)

**Files:**
- Create: `src/app/shared/cotizaciones/pricing-store.ts`
- Test: `src/app/shared/cotizaciones/pricing-store.spec.ts`

**Interfaces:**
- Consumes: `QuotesApi.getPricing`, `CotizacionAuthStore.adminToken()`, `normalizarCatalogo`.
- Produces: `PricingStore` con `items()` (signal `PricingItem[]`), `estado()` (signal
  `'idle'|'loading'|'listo'|'error'`), `cargar(): void`. Trae el catálogo **una vez**; llamadas
  repetidas a `cargar()` no re-piden si ya está `listo`.

- [ ] **Step 1: Escribir el test que falla**

```typescript
import { describe, it, expect, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { PricingStore } from './pricing-store';
import type { PricingCatalog } from './cotizaciones.model';

const CAT = {
  moneda: 'CLP', iva: 0.19, landings: [], kits: [], addons: [], identidad: [],
  piezas: [], horas: [],
} as PricingCatalog;

function make(getPricing: () => any) {
  const api = { getPricing: vi.fn(getPricing) } as any;
  const auth = { adminToken: () => 'tok' } as any;
  return { store: new PricingStore(api, auth), api };
}

describe('PricingStore', () => {
  it('carga el catálogo una sola vez', () => {
    const { store, api } = make(() => of(CAT));
    store.cargar();
    store.cargar();
    expect(api.getPricing).toHaveBeenCalledTimes(1);
    expect(store.estado()).toBe('listo');
  });

  it('si falla, queda en estado error y no revienta', () => {
    const { store } = make(() => throwError(() => new Error('core caído')));
    store.cargar();
    expect(store.estado()).toBe('error');
    expect(store.items()).toEqual([]);
  });
});
```

- [ ] **Step 2: Correr el test y verlo fallar**

Run: `npm test -- --run pricing-store`
Expected: FAIL — `PricingStore` no existe.

- [ ] **Step 3: Implementar `pricing-store.ts`**

```typescript
import { Injectable, inject, signal } from '@angular/core';
import { QuotesApi } from './quotes-api';
import { CotizacionAuthStore } from './auth-store';
import { normalizarCatalogo } from './catalogo';
import type { PricingItem } from './cotizaciones.model';

type Estado = 'idle' | 'loading' | 'listo' | 'error';

@Injectable({ providedIn: 'root' })
export class PricingStore {
  private readonly api: QuotesApi;
  private readonly auth: CotizacionAuthStore;

  readonly items = signal<PricingItem[]>([]);
  readonly estado = signal<Estado>('idle');

  // Constructor explícito para poder instanciarlo en tests sin TestBed.
  constructor(api?: QuotesApi, auth?: CotizacionAuthStore) {
    this.api = api ?? inject(QuotesApi);
    this.auth = auth ?? inject(CotizacionAuthStore);
  }

  /** Trae el catálogo una vez. No re-pide si ya está listo o cargando. */
  cargar(): void {
    if (this.estado() === 'listo' || this.estado() === 'loading') return;
    const token = this.auth.adminToken();
    if (!token) return;
    this.estado.set('loading');
    this.api.getPricing(token).subscribe({
      next: (cat) => {
        this.items.set(normalizarCatalogo(cat));
        this.estado.set('listo');
      },
      error: () => {
        this.items.set([]);
        this.estado.set('error');
      },
    });
  }
}
```

> Nota: el `inject()` dentro del constructor solo corre en runtime Angular; en el test se pasan los
> dos argumentos, evitando el contexto de inyección.

- [ ] **Step 4: Correr el test y verlo pasar**

Run: `npm test -- --run pricing-store`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/app/shared/cotizaciones/pricing-store.ts src/app/shared/cotizaciones/pricing-store.spec.ts
git commit -m "feat(cotizaciones): PricingStore con catálogo cacheado por sesión"
```

### Task 5: Vista de leads + enganche en el panel

**Files:**
- Create: `src/app/pages/admin/sections/leads/leads.ts`, `leads.html`, `leads.css`
- Modify: `src/app/pages/admin/admin.ts` (sumar vista `'leads'`)
- Modify: `src/app/pages/admin/admin.html` (tab a Leads; el detalle de conversión aterriza en `detalle`)

**Interfaces:**
- Consumes: `QuotesApi.listLeads`, `QuotesApi.convertLead`, `CotizacionAuthStore`, `Lead`, `LeadPage`.
- Produces: `AdminLeads` con `@Output() convertido = EventEmitter<string>` (id del borrador) y
  `@Output() loggedOut = EventEmitter<void>`.

- [ ] **Step 1: Implementar `leads.ts`**

```typescript
import {
  ChangeDetectionStrategy, Component, EventEmitter, OnInit, Output, inject, signal,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { QuotesApi } from '../../../../shared/cotizaciones/quotes-api';
import { CotizacionAuthStore } from '../../../../shared/cotizaciones/auth-store';
import type { Lead } from '../../../../shared/cotizaciones/cotizaciones.model';

const ESTADOS = ['nuevo', 'contactado', 'calificado', 'propuesta', 'ganado', 'perdido'] as const;

@Component({
  selector: 'app-admin-leads',
  templateUrl: './leads.html',
  styleUrl: './leads.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminLeads implements OnInit {
  @Output() convertido = new EventEmitter<string>();
  @Output() loggedOut = new EventEmitter<void>();

  private readonly api = inject(QuotesApi);
  private readonly authStore = inject(CotizacionAuthStore);

  readonly estados = ESTADOS;
  readonly filtro = signal<string | null>(null);
  readonly leads = signal<readonly Lead[]>([]);
  readonly loading = signal(true);
  readonly errorMsg = signal<string | null>(null);
  readonly convirtiendoId = signal<number | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  setFiltro(estado: string | null): void {
    this.filtro.set(estado);
    this.cargar();
  }

  cargar(): void {
    const token = this.authStore.adminToken();
    if (!token) { this.loggedOut.emit(); return; }
    this.loading.set(true);
    this.errorMsg.set(null);
    this.api.listLeads(this.filtro(), 20, token).subscribe({
      next: (page) => { this.leads.set(page.docs); this.loading.set(false); },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 401 || err.status === 403) {
          this.authStore.clearAdminToken(); this.loggedOut.emit(); return;
        }
        const backend = (err.error && (err.error.message || err.error.error)) as string | undefined;
        this.errorMsg.set(backend ?? `No se pudieron cargar los leads (${err.status || 'sin conexión'}).`);
      },
    });
  }

  convertir(lead: Lead): void {
    if (this.convirtiendoId()) return;
    const token = this.authStore.adminToken();
    if (!token) { this.loggedOut.emit(); return; }
    this.convirtiendoId.set(lead.id);
    this.errorMsg.set(null);
    this.api.convertLead(lead.id, token).subscribe({
      next: (borrador) => { this.convirtiendoId.set(null); this.convertido.emit(borrador.id); },
      error: (err: HttpErrorResponse) => {
        this.convirtiendoId.set(null);
        if (err.status === 401 || err.status === 403) {
          this.authStore.clearAdminToken(); this.loggedOut.emit(); return;
        }
        const backend = (err.error && (err.error.message || err.error.error)) as string | undefined;
        this.errorMsg.set(backend ?? `No se pudo convertir el lead (${err.status || 'sin conexión'}).`);
      },
    });
  }

  // El interés viene como JSON libre; se muestra un resumen legible si trae rubro/servicio.
  resumenInteres(lead: Lead): string {
    const i = lead.interes as Record<string, unknown> | null;
    if (!i || typeof i !== 'object') return '';
    const partes = [i['rubro'], i['servicio']].filter((x): x is string => typeof x === 'string');
    return partes.join(' · ');
  }
}
```

- [ ] **Step 2: Implementar `leads.html`**

Seguir el estilo de `dashboard.html` (Tailwind, tokens `ink`/`neon`/`muted`). Estructura mínima:

```html
<section class="px-4 py-8">
  <div class="mx-auto max-w-4xl">
    <h2 class="font-display text-2xl font-extrabold text-ink mb-1">Leads</h2>
    <p class="text-muted text-sm mb-5">Elige un lead y conviértelo en un borrador de cotización.</p>

    <div class="flex flex-wrap gap-2 mb-5">
      <button type="button" (click)="setFiltro(null)"
        [class.bg-ink]="filtro() === null" [class.text-white]="filtro() === null"
        class="rounded-full border border-paper px-3 py-1 text-sm">Todos</button>
      @for (e of estados; track e) {
        <button type="button" (click)="setFiltro(e)"
          [class.bg-ink]="filtro() === e" [class.text-white]="filtro() === e"
          class="rounded-full border border-paper px-3 py-1 text-sm capitalize">{{ e }}</button>
      }
    </div>

    @if (errorMsg()) {
      <div class="mb-4 rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-red-700 text-sm">
        {{ errorMsg() }}
      </div>
    }

    @if (loading()) {
      <p class="text-muted text-sm">Cargando leads…</p>
    } @else if (leads().length === 0) {
      <p class="text-muted text-sm">No hay leads{{ filtro() ? ' en estado ' + filtro() : '' }}.</p>
    } @else {
      <ul class="space-y-3">
        @for (lead of leads(); track lead.id) {
          <li class="flex items-center justify-between gap-4 rounded-2xl border border-paper bg-white px-5 py-4">
            <div class="min-w-0">
              <p class="font-semibold text-ink truncate">{{ lead.nombre }}</p>
              <p class="text-sm text-muted truncate">
                {{ resumenInteres(lead) }}
                @if (lead.origen) { <span class="opacity-70">· {{ lead.origen }}</span> }
                <span class="opacity-70">· {{ lead.estado }}</span>
              </p>
            </div>
            <button type="button" (click)="convertir(lead)" [disabled]="!!convirtiendoId()"
              class="shrink-0 rounded-full bg-neon text-ink px-4 py-2 text-sm font-semibold disabled:opacity-50">
              @if (convirtiendoId() === lead.id) { Convirtiendo… } @else { Convertir a borrador }
            </button>
          </li>
        }
      </ul>
    }
  </div>
</section>
```

- [ ] **Step 3: `leads.css`** — dejar vacío o con estilos mínimos (Tailwind cubre el layout).

- [ ] **Step 4: Enganchar en `admin.ts`**

- Sumar `'leads'` al tipo: `type AdminView = 'login' | 'dashboard' | 'nueva' | 'detalle' | 'leads';`
- Importar `AdminLeads` y agregarlo a `imports`.
- Agregar métodos:

```typescript
  goToLeads(): void {
    this.view.set('leads');
  }

  onLeadConvertido(quoteId: string): void {
    this.selectedQuoteId.set(quoteId);
    this.view.set('detalle');
  }
```

- [ ] **Step 5: Enganchar en `admin.html`**

- En la cabecera del panel (donde están los accesos a dashboard/nueva), sumar un botón/tab:
  `<button (click)="goToLeads()">Leads</button>` (siguiendo el markup de los tabs existentes).
- Renderizar la vista:

```html
@if (view() === 'leads') {
  <app-admin-leads (convertido)="onLeadConvertido($event)" (loggedOut)="cerrarSesion()"></app-admin-leads>
}
```

- [ ] **Step 6: Verificar que compila y arranca**

Run: `npm run build`
Expected: build OK.

- [ ] **Step 7: Commit**

```bash
git add src/app/pages/admin/sections/leads/ src/app/pages/admin/admin.ts src/app/pages/admin/admin.html
git commit -m "feat(panel): vista de leads con filtro y convertir a borrador"
```

### Task 6: Agregar opción + selector de catálogo en el detalle

**Files:**
- Modify: `src/app/pages/admin/sections/detalle/detalle.ts`
- Modify: `src/app/pages/admin/sections/detalle/detalle.html`

**Interfaces:**
- Consumes: `QuotesApi.addOption`, `PricingStore` (`items()`, `estado()`, `cargar()`),
  `CreateOptionRequest`, `PricingItem`.
- Produces: en `AdminDetalle`, el modo "agregar opción" y un método `aplicarItemCatalogo(item, form)`
  que precarga el formulario de opción.

- [ ] **Step 1: Inyectar `PricingStore` y cargar al abrir el detalle**

En `detalle.ts`: `private readonly pricing = inject(PricingStore);` y en `ngOnInit()` (o `loadQuote`
success) llamar `this.pricing.cargar();`. Exponer `readonly catalogo = this.pricing.items;` y
`readonly catalogoEstado = this.pricing.estado;`.

- [ ] **Step 2: Agregar el modo "agregar opción"**

Reutilizar `optionForm` (ya existe). Agregar:

```typescript
  readonly agregandoOpcion = signal(false);

  startAddOption(): void {
    const arr = this.optionForm.get('features') as FormArray;
    while (arr.length > 0) arr.removeAt(0);
    this.optionForm.reset({
      titulo: '', descripcion: '', precio: 0, precioMensual: null,
      currency: 'CLP', recomendado: false,
    });
    this.editingOptionId.set(null);
    this.agregandoOpcion.set(true);
  }

  cancelAddOption(): void {
    this.agregandoOpcion.set(false);
  }

  // Precarga el formulario de opción desde un ítem del catálogo del Core.
  aplicarItemCatalogo(item: PricingItem): void {
    this.optionForm.patchValue({
      titulo: item.label,
      precio: item.precioNeto,
      precioMensual: item.mensualNeto,
    });
  }

  saveNewOption(): void {
    if (this.saving()) return;
    const token = this.authStore.adminToken();
    if (!token) return;
    this.saving.set(true);
    this.saveError.set(null);
    const raw = this.optionForm.getRawValue();
    const body: CreateOptionRequest = {
      titulo: raw.titulo,
      descripcion: raw.descripcion || undefined,
      precio: Number(raw.precio),
      precioMensual: raw.precioMensual ? Number(raw.precioMensual) : null,
      currency: raw.currency,
      recomendado: raw.recomendado,
      features: (raw.features as string[]).filter((f) => f.trim().length > 0),
    };
    this.api.addOption(this.quoteId, body, token).subscribe({
      next: (updated) => { this.quote.set(updated); this.saving.set(false); this.agregandoOpcion.set(false); },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        if (err.status === 401 || err.status === 403) {
          this.authStore.clearAdminToken(); this.loggedOut.emit();
        } else {
          this.saveError.set(`No se pudo agregar la opción (${err.status || 'sin conexión'}).`);
        }
      },
    });
  }
```

Añadir el import de tipos: `CreateOptionRequest`, `PricingItem`, y `PricingStore`.

- [ ] **Step 3: HTML — botón "Agregar opción", formulario y selector**

En `detalle.html`, después de la lista de opciones, agregar el bloque de agregar. El **selector de
catálogo** es un `<select>` que al cambiar llama `aplicarItemCatalogo`. Reutilizar la estructura del
formulario de editar opción que ya existe. Selector (va tanto en agregar como en editar):

```html
@if (catalogoEstado() === 'listo') {
  <label class="block text-sm font-medium text-ink mb-1">Desde el catálogo</label>
  <select (change)="aplicarItemCatalogo(catalogo()[$any($event.target).selectedIndex - 1])"
    class="w-full rounded-lg border border-paper px-3 py-2 mb-3">
    <option value="">— Elegir del catálogo (precarga precio) —</option>
    @for (item of catalogo(); track $index) {
      <option>{{ item.categoria }} · {{ item.label }} — {{ formatPrice(item.precioConIva, 'CLP') }} c/IVA</option>
    }
  </select>
} @else if (catalogoEstado() === 'error') {
  <p class="text-sm text-muted mb-3">Catálogo no disponible; ingresa el precio a mano.</p>
}
```

> Ojo con el índice: `selectedIndex - 1` compensa la primera opción "— Elegir —". Si el executor
> prefiere robustez, usar `[value]="$index"` en cada `<option>` y leer `(change)` con el value.
> Preferir la variante `[value]` si hay cualquier duda.

Botón para abrir el modo:

```html
@if (!agregandoOpcion() && !editingOptionId()) {
  <button type="button" (click)="startAddOption()"
    class="rounded-full bg-ink text-white px-4 py-2 text-sm font-semibold">+ Agregar opción</button>
}
```

El formulario de agregar reutiliza los mismos campos que editar (título, descripción, precio,
precioMensual, features, recomendado), con botones **Guardar** (`saveNewOption()`) y **Cancelar**
(`cancelAddOption()`).

- [ ] **Step 4: Verificar que compila**

Run: `npm run build`
Expected: build OK.

- [ ] **Step 5: Commit**

```bash
git add src/app/pages/admin/sections/detalle/
git commit -m "feat(panel): agregar opción a un borrador con selector de catálogo"
```

### Task 7: Selector de catálogo en "Nueva cotización"

**Files:**
- Modify: `src/app/pages/admin/sections/nueva/nueva.ts`
- Modify: `src/app/pages/admin/sections/nueva/nueva.html`

**Interfaces:**
- Consumes: `PricingStore`, `PricingItem`.
- Produces: método `aplicarItemCatalogo(optionIndex, item)` que precarga la opción `optionIndex`.

- [ ] **Step 1: Inyectar `PricingStore` y cargar**

En `nueva.ts`: `private readonly pricing = inject(PricingStore);`, en el constructor o `ngOnInit`
llamar `this.pricing.cargar();`. Exponer `readonly catalogo = this.pricing.items;` y
`readonly catalogoEstado = this.pricing.estado;`.

- [ ] **Step 2: Método de precarga por opción**

```typescript
  aplicarItemCatalogo(optionIndex: number, item: PricingItem): void {
    this.optionGroup(optionIndex).patchValue({
      titulo: item.label,
      precio: item.precioNeto,
      precioMensual: item.mensualNeto,
    });
  }
```

- [ ] **Step 3: HTML — selector por cada opción**

En `nueva.html`, dentro del bloque que repite cada opción (`optionIndices()`), antes del campo de
título, agregar el mismo `<select>` de la Task 6 pero llamando
`aplicarItemCatalogo(i, catalogo()[...])`. Usar la variante `[value]="$index"` para evitar el
desfase de índice:

```html
@if (catalogoEstado() === 'listo') {
  <select (change)="aplicarItemCatalogo(i, catalogo()[+$any($event.target).value])"
    class="w-full rounded-lg border border-paper px-3 py-2 mb-2">
    <option value="-1">— Desde el catálogo (precarga precio) —</option>
    @for (item of catalogo(); track $index) {
      <option [value]="$index">{{ item.categoria }} · {{ item.label }}</option>
    }
  </select>
}
```

(Guardar el `aplicarItemCatalogo` de valor `-1` con un guard: si el índice es `< 0`, no hacer nada.)

- [ ] **Step 4: Guard del índice**

Ajustar el método:

```typescript
  aplicarItemCatalogo(optionIndex: number, item: PricingItem | undefined): void {
    if (!item) return;
    this.optionGroup(optionIndex).patchValue({
      titulo: item.label, precio: item.precioNeto, precioMensual: item.mensualNeto,
    });
  }
```

- [ ] **Step 5: Verificar que compila**

Run: `npm run build`
Expected: build OK.

- [ ] **Step 6: Commit**

```bash
git add src/app/pages/admin/sections/nueva/
git commit -m "feat(panel): selector de catálogo al crear una cotización"
```

### Task 8: E2E — listar leads → convertir → aterrizar en detalle

**Files:**
- Modify: `e2e/admin.spec.ts`

**Interfaces:**
- Consumes: helpers existentes `loginViaStorage`, `API_BASE`, `page.route()`.

- [ ] **Step 1: Escribir el test e2e**

Agregar a `e2e/admin.spec.ts` (mockeando `/api/admin/leads` y `/api/admin/quotes/from-lead`):

```typescript
test('convierte un lead en borrador y abre el detalle', async ({ page }) => {
  await page.route(`${API_BASE}/api/admin/leads*`, (route) =>
    route.fulfill({
      json: { docs: [{ id: 7, nombre: 'Jorge', email: null, telefono: null,
                        mensaje: null, origen: 'bot', estado: 'nuevo',
                        interes: { rubro: 'Centro de eventos', servicio: 'Sitio web' } }],
              total: 1, page: 1, totalPages: 1 },
    }),
  );
  await page.route(`${API_BASE}/api/admin/quotes/from-lead`, (route) =>
    route.fulfill({
      json: { id: 'q1', codigo: '6k9867jtqm', claveTexto: 'x', clientName: 'Jorge',
              clientEmail: null, notes: 'Lead del CRM del Core', titulo: null, mensaje: null,
              imagenes: null, status: 'PENDING', canSelect: true,
              createdAt: '2026-08-07T20:00:00Z', expiresAt: '2026-08-22T20:00:00Z',
              sentAt: null, selectedOptionId: null, selectedAt: null, rejectedAt: null,
              ivaPct: 19, options: [], history: [] },
    }),
  );
  // El detalle vuelve a pedir el quote por id:
  await page.route(`${API_BASE}/api/admin/quotes/q1`, (route) =>
    route.fulfill({ json: { id: 'q1', codigo: '6k9867jtqm', clientName: 'Jorge', status: 'PENDING',
      options: [], history: [], ivaPct: 19, expiresAt: '2026-08-22T20:00:00Z',
      createdAt: '2026-08-07T20:00:00Z', canSelect: true, claveTexto: 'x' } }),
  );
  // Catálogo (para que el detalle no falle al cargar el selector):
  await page.route(`${API_BASE}/api/admin/pricing`, (route) =>
    route.fulfill({ json: { moneda: 'CLP', iva: 0.19, landings: [], kits: [], addons: [],
      identidad: [], piezas: [], horas: [] } }),
  );

  await loginViaStorage(page);
  await page.getByRole('button', { name: 'Leads' }).click();
  await expect(page.getByText('Jorge')).toBeVisible();
  await page.getByRole('button', { name: /Convertir/ }).click();
  await expect(page.getByText('6k9867jtqm')).toBeVisible(); // ya en el detalle del borrador
});
```

- [ ] **Step 2: Correr el e2e**

Run: `npm start` (en otra terminal) y luego `npm run e2e -- admin.spec.ts`
Expected: PASS. (Ajustar selectores si el markup real difiere; el test debe reflejar el DOM, no al revés.)

- [ ] **Step 3: Commit**

```bash
git add e2e/admin.spec.ts
git commit -m "test(e2e): convertir un lead en borrador desde el panel"
```

### Task 9: Guía del flujo + rama lista para merge

**Files:**
- Create: `docs/guias/panel-leads-a-cotizacion.md` (en `webiados/webiados`)

- [ ] **Step 1: Escribir la guía**

Seguir el formato de `docs/guias/` del repo frontend (las seis secciones: qué hace, dónde se ve,
cómo se prende/apaga, cómo se demuestra en 60s, qué NO hace, si algo sale mal). Cubrir: entrar al
panel → Leads → filtrar → Convertir → detalle → Agregar opción con el selector → Enviar. Español de Chile.

- [ ] **Step 2: Verificación manual contra prod**

Con `npm start`, entrar a `/admin`, iniciar sesión de verdad (Felipe), ir a Leads, convertir un lead
de prueba, y comprobar que el selector precarga precios reales del Core. Anotar el resultado.

- [ ] **Step 3: Commit + push de la rama**

```bash
git add docs/guias/panel-leads-a-cotizacion.md
git commit -m "docs(guias): flujo de lead a cotización en el panel"
git push -u origin feat/panel-leads-y-catalogo
```

- [ ] **Step 4: Avisar a Felipe** para que revise la rama y la mergee a `main` (Vercel auto-despliega).
  Reportar en el buzón. No mergear a `main` sin su OK.

---

## Self-Review (hecho al escribir el plan)

- **Cobertura del spec:** §2 backend → Task 1. §3.1 modelos → Task 2. §3.2 API → Task 3. §3.6
  PricingStore → Task 4. §3.3 vista leads → Task 5. §3.4 agregar opción + selector → Task 6. §3.5
  selector en nueva → Task 7. §5 tests → Tasks 1,2,4,8. §5 guías → Tasks 1(step 8), 9. §3.7 fallback →
  Tasks 4,6. Rama/deploy → prep Task 2, Tasks 1(step 10), 9.
- **Sin placeholders:** cada step de código trae el código real.
- **Consistencia de tipos:** `normalizarCatalogo`, `PricingItem`, `PricingStore.items()/estado()/cargar()`,
  `listLeads/convertLead/getPricing/addOption`, `aplicarItemCatalogo` usados con las mismas firmas en
  todas las tasks.
