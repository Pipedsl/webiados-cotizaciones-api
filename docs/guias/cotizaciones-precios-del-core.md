# Cotizaciones — de dónde salen los precios

## Qué hace
Los precios de los kits y agregados no se escriben a mano en cada cotización: salen de un catálogo
único que publica el Core (que a su vez sale de `pricing.md`). Así el precio que ve el cliente en una
cotización es el mismo que muestra el sitio, sin diferencias.

## Dónde se ve
- **El catálogo** lo entrega el Core en `core.webiados.com/api/v1/pricing`.
- **En este servicio** se lee en `GET /api/admin/pricing` (con token de admin), para que el panel
  arme opciones eligiendo un ítem con el monto ya puesto. (El "elegir ítem" en el panel es frontend
  pendiente; hoy existe el endpoint que lo va a alimentar.)

## Cómo se prende / apaga
Viene configurado por defecto (`PRICING_URL` apunta al Core). No hay que prender nada.
- Si el Core no responde, el servicio **sirve el último catálogo que leyó** y lo avisa en los logs.
- Si nunca alcanzó a leer uno, **falla en vez de inventar** un precio.

## Cómo se demuestra (60 segundos)
1. `GET /api/admin/pricing` (con token de admin) devuelve los kits y agregados.
2. Muestra que cada monto viene en cuatro monedas —**UF, neto, con IVA y dólar**— ya calculadas por el
   Core, con la UF y el dólar del día.
3. Señala el `primerAnioMonto` de un kit: es **instalación + 12 mensualidades**, lo que el cliente
   desembolsa el primer año.

## Qué NO hace
- **No convierte monedas.** Usa las que ya calculó el Core, a propósito: si este servicio convirtiera
  y el sitio también, tarde o temprano difieren en un peso y dirían números distintos.
- **No es la fuente de los precios:** la fuente es `pricing.md` (en el Core). Acá solo se leen.
- **No cambia una cotización ya enviada:** cambiar `pricing.md` afecta las cotizaciones nuevas, no las
  que un cliente ya recibió (esas son una foto).
- **No expone precios internos:** el Core ya filtra las secciones privadas de `pricing.md`.

## Si algo sale mal
- **Un precio se ve raro o desactualizado:** el catálogo pudo quedar servido de caché porque el Core no
  respondió. Revisa que `core.webiados.com/api/v1/pricing` conteste, y los logs de este servicio (avisa
  cuando sirve caché).
- **"No se pudo leer el catálogo… y no hay ninguno cacheado":** el Core está caído y este servicio
  todavía no había leído ninguno. Se arregla solo cuando el Core vuelve.
