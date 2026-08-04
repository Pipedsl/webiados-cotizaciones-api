# Sprint 3 — Recibir los leads del outbound (diseño, pendiente de decisión)

> Escrito el 2026-08-03. Contexto: [`SIGUIENTE.md`](../SIGUIENTE.md) §4, [`TASKLIST.md`](../TASKLIST.md).
> **No implementado a propósito:** toca el flujo de creación (load-bearing) y depende de un
> contrato que decide Felipe. Este doc deja el diseño listo para aprobar, no código especulativo.

## DoD
Un lead que responde se convierte en `Quote` **sin retipear los datos**. El formulario público de
`webiados.com` termina acá.

## El bloqueo real (hallazgo)
Hoy `CreateQuoteRequest.options` es `@NotEmpty`: **una cotización no puede existir sin al menos una
opción**. Un lead no trae opciones ni precios — trae contacto y contexto. Así que "lead → Quote"
exige poder crear un **borrador** de cotización solo con los datos del cliente, que el admin
completa después (ahora con los precios del catálogo del Core, ya integrado en §3).

## Propuesta
1. **Estado borrador.** Permitir crear un `Quote` en `PENDING` **sin opciones**, con nombre, correo
   y contexto (rubro/notas). El admin le agrega opciones con `POST /{id}/options` (ya existe) y
   recién ahí la envía. No se puede *enviar* una cotización sin opciones (validar en `send`).
2. **Endpoint de ingreso:** `POST /api/admin/quotes/from-lead` con `{ clientName, clientEmail,
   rubro?, notas?, origen? }`. Mapea el lead a un borrador y devuelve su `id` para editarlo.
3. **El nombre se normaliza** al guardar (ya lo hace `Formatos.nombre`), así el borrador ya sale
   bien en panel y correo.

## Decisiones que necesita Felipe (no las tomo yo)
- **¿Quién hace el POST?** ¿El formulario de `webiados.com` pega directo a este servicio, o pasa
  por el Core / `buscadorLeads`? Eso define **auth**: hoy todo `/api/admin/**` pide JWT de admin;
  un formulario público necesitaría otra puerta (token de servicio, o un endpoint público acotado
  con rate-limit, distinto del admin).
- **¿Qué campos trae el lead?** `buscadorLeads` es un proyecto Python aparte con datos personales;
  no inventé su esquema. Hay que fijar el set mínimo ({nombre, correo, rubro, teléfono?}).
- **¿Se acepta el borrador sin opciones?** Es el cambio de fondo (relaja `@NotEmpty`). Si no se
  quiere tocar eso, la alternativa es crear el borrador con una opción placeholder — más feo.

## Verificación (cuando se apruebe)
Un lead entra por el endpoint → aparece un borrador en el panel con los datos ya puestos → el admin
agrega opciones del catálogo y lo envía, sin haber retipeado nombre ni correo.
