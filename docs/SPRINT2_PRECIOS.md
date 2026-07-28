# Sprint 2 — Una sola fuente de precios (replanteo)

> Escrito el 2026-07-27, tras la auditoría del código. Reemplaza la tarea 2.2 tal como
> estaba redactada. Contexto: [`AUDITORIA.md`](AUDITORIA.md), [`../TASKLIST.md`](../TASKLIST.md).

## Por qué se replantea

La tarea 2.2 decía: *"sincronizar `Selection` contra el endpoint de precios del Core"*.
**No es ejecutable.** `Selection` no es un catálogo de precios: es una **bitácora**. En el
código (`domain/Selection.java`) es una fila que registra que un cliente eligió una opción
—con `quote`, `option`, `kind` y `createdAt`— y `SelectionKind` solo tiene `INITIAL` y
`UPGRADE`. Sincronizar una bitácora contra una lista de precios no significa nada.

Dicho de otro modo: **en este servicio no existe hoy ninguna entidad de catálogo.** Los
precios se escriben a mano, opción por opción, en el panel. Por eso las dos cotizaciones
reales se armaron con montos que no salen de `pricing.md` (Kit Tienda vale $890.000 en
`pricing.md`; a la pastelería se le cotizó $1.040.000 armado por líneas sueltas).

## Cuál es la fuente correcta del catálogo

Ya existe y es autoritativa. **No hay que crearla acá — hay que consumirla.**

- **La fuente de verdad es `pricing.md`.** Los kits están en §3, los agregados en §4 (cada
  uno con un `slug` en backticks). Todos con instalación (`setup`) y `mensual`, en CLP,
  **netos, sin IVA**.
- **El Core ya la publica:** `GET /api/v1/pricing` (`core/src/endpoints/pricing.ts`)
  devuelve `{ addons: [{ slug, etiqueta, setup, mensual }], moneda: "CLP", incluyeIva: false }`.
  Los kits se leen con `parsearKit()`. Verificado: el parser solo reconoce filas con `slug`
  en backticks y **no hay ninguna tabla así debajo de la línea 338** (`# 🔒 PARTE INTERNA`),
  así que el endpoint no expone nada de §10-15.

## La entidad correcta: un catálogo de solo lectura, no una fuente nueva

Propuesta: una entidad de lectura, `PriceItem` (o `ItemCatalogo`), que **espeja** lo que
devuelve el Core. **No es una fuente de verdad nueva** —eso volvería a partir el problema
en dos— sino un *read-through* del endpoint del Core.

```
PriceItem
  slug        String   // "agenda", "bot_ia", "tienda"  (clave estable)
  etiqueta    String   // "Módulo de reservas"
  tipo        enum { KIT, ADDON }
  setup       BigDecimal   // neto, CLP
  mensual     BigDecimal   // neto, CLP, nullable (— en pricing.md → null)
```

Reglas que lo mantienen honesto:

1. **Nunca se persiste como canónico.** Se cachea del endpoint del Core con un TTL corto
   (p. ej. 1 h) o se refresca a demanda. Si el Core no responde, se sirve el último
   cacheado y se avisa; nunca se inventa un precio.
2. **El dinero sigue siendo entero y neto.** El IVA lo calcula la cotización con su
   `ivaPct` (ya implementado). El Core publica `incluyeIva: false`; se respeta.
3. **No se toca `Selection`.** Sigue siendo la bitácora que ya es.

## Cómo se enchufa al armado de cotizaciones

Hoy `OptionRequest` trae `precio` y `precioMensual` como números libres. El cambio:

- Al armar una opción, el admin **elige un `PriceItem` por su `slug`**; `precio` y
  `precioMensual` se **prellenan desde el valor del Core**, no se teclean.
- El texto (título, descripción, features) sigue siendo editable —una cotización es más
  que un precio— pero **el monto viene del catálogo**.
- Para un combo (kit + agregados), se suman los `setup` y los `mensual` de los `slug`
  elegidos. Esa suma es el `precio` / `precioMensual` de la opción.

Con esto, "improvisar un monto en una reunión" deja de ser posible sin editar `pricing.md`.

## Tareas 2.1–2.4, reescritas

| # | Tarea | Verificación |
|---|---|---|
| 2.1 | Cliente HTTP (`RestClient`) contra `GET /api/v1/pricing` del Core, con timeout y fallback al último cacheado | Un test levanta un stub del Core y el servicio lee `{slug, setup, mensual}` |
| 2.2 | Entidad `PriceItem` como read-through del endpoint (kits + addons), con TTL/refresh | Cambiar un precio en `pricing.md` se refleja en el catálogo tras el refresh |
| 2.3 | El panel arma opciones eligiendo `slug`, con `precio`/`precioMensual` prellenados | Crear una opción desde el catálogo copia el monto del Core, no uno tecleado |
| 2.4 | Test: ningún monto de negocio hardcodeado en el código Java | `grep` de literales de precio en `src/main` sin coincidencias; los montos vienen del Core o del payload de carga histórica |

**DoD:** cambiar un precio en `pricing.md` cambia lo que ve el cliente **en las
cotizaciones nuevas**, sin tocar código.

> ⚠️ **Las cotizaciones ya enviadas NO cambian.** Una cotización es un acuerdo con una
> fecha: su precio es un *snapshot*. Cambiar `pricing.md` afecta lo que se cotiza de acá en
> adelante, no lo que un cliente ya recibió. Esto es correcto y hay que dejarlo explícito
> para no confundir el DoD.

## Dónde vive la garantía de §10-15 (interno)

El filtro de las secciones internas **vive en el Core**, no acá: este servicio consume el
endpoint, que ya devuelve solo lo público. La garantía es hoy *de hecho* (no hay tablas con
`slug` bajo la línea 338), no *estructural*. Recomendación para el Core, ya anotada en
`AUDITORIA.md` §3 #5: cortar el markdown en la línea 338 antes de parsear, para que sea
imposible por construcción y no por vigilancia.
