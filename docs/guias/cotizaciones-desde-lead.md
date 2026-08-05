# Cotizaciones — convertir un lead en cotización

## Qué hace
Cuando alguien deja sus datos en el formulario del sitio, queda como "lead" en el CRM del Core. Con
esto, tomas ese lead y creas una cotización con sus datos ya puestos (nombre, correo, lo que pidió),
sin volver a escribirlos. La cotización nace como **borrador** para que le agregues las opciones.

## Dónde se ve
- **Los leads** viven en el CRM del Core (`core.webiados.com`).
- **La conversión** la hace este servicio: el panel de cotizaciones lista los leads y, al elegir uno,
  crea el borrador. (El botón en el panel es trabajo de frontend pendiente; ver "Si algo sale mal".)

## Cómo se prende / apaga
Necesita **una sola cosa de configuración**: la llave del Core (`CORE_API_KEY`) puesta en Railway.
- **Con la llave:** convertir un lead funciona.
- **Sin la llave:** el resto del servicio anda igual, pero convertir un lead falla con un aviso claro
  ("requiere la llave del Core"). No rompe nada más.

## Cómo se demuestra (60 segundos)
> Requiere `CORE_API_KEY` puesta y el botón en el panel (frontend). Mientras tanto, se demuestra por API:
1. Con un lead ya cargado en el CRM del Core, anota su número (id).
2. `POST /api/admin/quotes/from-lead` con `{ "leadId": <id> }` (con el token de admin).
3. Aparece una cotización nueva en **estado Pendiente**, con el nombre y correo del lead ya puestos y su
   mensaje en las notas internas.
4. Le agregas opciones con precios del catálogo y la envías como cualquier otra.

## Da lo mismo si el lead es viejo
Sirve **cualquier** lead del CRM, tenga la antigüedad que tenga. El servicio se lo pide al Core por su
número exacto (`GET /api/v1/leads/{id}`).

> Antes no era así: el servicio traía los 200 leads más recientes y buscaba el tuyo entre ellos. En
> cuanto el CRM pasara de 200 —el formulario del sitio, el bot de WhatsApp y las demos suman rápido—
> cualquier prospecto de hace unos días habría dejado de ser convertible, con un error que no explicaba
> por qué. Arreglado el 2026-08-05.

## Qué NO hace
- **No recibe el formulario.** El formulario del sitio postea al **Core**, no acá. Este servicio solo
  **lee** el lead cuando lo conviertes.
- **No trae precios ni opciones:** un lead todavía no tiene nada de eso. Por eso nace como borrador.
- **No se puede enviar un borrador vacío:** el sistema se niega hasta que tenga al menos una opción.
- **No inventa datos:** toma los campos tal como están en el CRM del Core.

## Si algo sale mal
- **"requiere la llave del Core (CORE_API_KEY)":** falta la llave en Railway. 🚧 La pone Felipe.
- **"El lead N no existe en el CRM del Core, o es de otro sitio":** el número está malo, o ese lead es
  de otro cliente. El Core responde igual en los dos casos a propósito (no le confirma a nadie que un
  lead existe en otra parte). Revisa el número en el CRM.
- **"No se pudo leer el lead N del Core":** el Core no respondió o se cayó. **No** quiere decir que el
  lead no exista; espera un momento y vuelve a intentarlo.
- **No hay botón en el panel todavía:** la lista de leads y el botón "convertir" son trabajo de frontend
  en `webiados/webiados`. Hoy la conversión existe en el backend, lista para engancharse.
