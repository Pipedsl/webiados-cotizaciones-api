# SIGUIENTE — Cotizaciones API

> **Prioridad en el ecosistema: P0** · Decidido 2026-07-27
> Contexto global: `../Demos-Webiados-Clientes/CONTROL.md`
> Estrategia comercial: `../Demos-Webiados-Clientes/docs/estrategia_comercial.md`
> **Sin fechas: por orden.**

## Por qué este repo es P0

Es una de las **cuatro máquinas construidas y apagadas**. Este servicio está **vivo y
desplegado** (`cotiza.webiados.com`, Railway, último commit 2026-07-22) — y sin embargo las dos
cotizaciones reales del mes, Macarena Larraín y la pastelería Vientos del Sur, **se escribieron
a mano en Markdown y se exportaron a PDF**.

Eso significa tres cosas, todas malas: se pierde el historial comercial, se improvisan montos
fuera de `pricing.md`, y no hay forma de saber cuántas cotizaciones se enviaron ni cuántas se
cerraron. Sin ese dato no se puede medir la meta de **🎯 10 conversaciones reales al mes**.

**El trabajo acá no es construir. Es hacer que se use.**

---

## Qué hacer, en orden

### 1. Cargar las cotizaciones reales que existen *(P0)*

Meter en el sistema las que ya se enviaron, para que el historial arranque con datos de verdad:

| Cliente | Estado | Dónde está el original |
|---|---|---|
| Macarena Larraín — Opción C, $380.000 + IVA | `ACCEPTED` | `../Demos-Webiados-Clientes/docs/Cotizaciones/` |
| Pastelería Vientos del Sur — 3 opciones | `SENT` | `../Demos-Webiados-Clientes/docs/Cotizaciones/Cotizacion_Pasteleria_VientosdelSur.md` |

**Criterio de verificación:** entrar al panel y ver las dos, con su estado correcto.

### 2. Que la próxima cotización nazca acá, no en Markdown *(P0)*

Antes de agregar una sola función nueva, la próxima cotización que Felipe envíe tiene que salir
de este sistema. Si algo lo impide, **ese impedimento es el trabajo** — no una excusa para
volver al Markdown.

Lo más probable: falta que el PDF/landing que genera se vea tan bien como el Markdown exportado.
`V3__add_landing_fields.sql` ya agregó `titulo`, `mensaje` e `imagenes` para eso.

**Criterio de verificación:** una cotización enviada a un cliente real desde el sistema.

### 3. Leer los precios de `pricing.md`, no tenerlos escritos acá *(P1)*

Regla dura de Webiados: **los precios se leen de `pricing.md`** vía `GET /api/v1/pricing` del
Core. Hoy las `Selection` viven en la base de este servicio: se van a desincronizar.

- Sincronizar `Selection` contra el endpoint de precios del Core, o
- documentar explícitamente por qué no se puede y cómo se mantienen alineados.

**Nunca hardcodear un monto ni improvisarlo en una reunión.**

### 4. Recibir los leads del outbound *(P1)*

`../buscadorLeads/` genera leads y `../Demos-Webiados-Clientes/core/` los captura. Un lead que
responde tiene que poder convertirse en `Quote` sin recapturar los datos a mano.

**Criterio de verificación:** un lead del buscador termina como cotización sin retipear nada.

### 5. Alimentar el dashboard de agencia *(P2)*

Cuando exista el dashboard (fase 2, en el Core), este servicio es la fuente del embudo:
cotizaciones enviadas, aceptadas, rechazadas y monto en pipeline. **No duplicar ese dominio en
el Core** — se consume desde acá.

---

## Qué NO hacer

- **No reescribirlo.** Funciona, está desplegado y tiene Flyway, JWT y rate limiting. El problema
  es de adopción, no de código.
- **No agregar CRM, facturación ni gestión de proyectos acá.** Eso es el dashboard de agencia y
  vive en el Core. Este servicio hace cotizaciones.
- **No exponer nada sin auth** salvo el formulario público que ya existe.

## Reglas

1. Migraciones con Flyway, siempre. Probadas sobre base vacía y sobre base con datos.
2. El dinero es entero. IVA 19%. Prohibido `float` para montos.
3. No se toca producción sin autorización explícita de Felipe.
