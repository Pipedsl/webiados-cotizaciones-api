# Carga inicial del histórico

Las dos cotizaciones reales que hoy están en Markdown/PDF, listas para entrar al sistema
con su **estado y fecha reales** (Sprint 1, tareas 1.2 y 1.3).

| Archivo | Cliente | Estado final | Emitida |
|---|---|---|---|
| `macarena.json` | Macarena Larraín | `SELECTED` (aceptó Opción C) | 2026-07-24 |
| `vientos-del-sur.json` | Pastelería Vientos del Sur | `SENT` | 2026-07-27 * |

\* La fecha de Vientos del Sur es una suposición (el Markdown no lleva fecha impresa). Si
fue otra, corregir con `PATCH /api/admin/quotes/{id}` antes de medir la tasa de cierre.

Los montos están **verificados contra los originales** y sin IVA escrito a mano: el IVA y
los totales los calcula el servicio. El ensayo `CargaHistoricaIT` corre estos mismos JSON
contra un Postgres real y compara los totales con el PDF y el Markdown.

## Cómo se corre

**Requisito previo: V4 tiene que estar desplegada.** Los endpoints `mark-sent` y los campos
nuevos (`status`, `precio_mensual`, `iva_pct`) no existen en la versión que hoy corre en
Railway. **El deploy lo autoriza Felipe.**

Primero, ver qué haría sin escribir nada:

```bash
DRY_RUN=1 ./cargar.sh
```

Después, la carga real (la corre **Felipe**, no el asistente):

```bash
export ADMIN_EMAIL='felipe@webiados.com'
export ADMIN_PASSWORD='...'          # ADMIN_BOOTSTRAP_PASSWORD de Railway
./cargar.sh
```

El script hace login, crea las dos con su fecha real, las marca como entregadas **sin
mandar correo a nadie** (`mark-sent` — ya recibieron su cotización hace semanas), y registra
la elección de Macarena por el flujo del cliente para que quede también en la bitácora.

## Advertencias

1. **No es idempotente.** Correrlo dos veces crea dos veces. Revisa el panel antes de
   repetir. Escribe en la base de producción.
2. **Registrar la elección de Macarena dispara el aviso interno** a `NOTIFY_TO`
   (`contacto@webiados.com`). Al cliente no le llega nada.
3. **Guarda los códigos y claves** que imprime al final: la clave en texto se muestra una
   sola vez (aunque también queda en `clave_texto`, visible en el detalle del panel).

## Verificación (cierra 1.2 y 1.3)

Entrar al panel en `webiados.com/admin` y confirmar que aparecen las dos: Macarena en
`SELECTED`, la pastelería en `SENT`.
