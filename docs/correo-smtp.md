# Correo saliente (SMTP)

> **Decisión (2026-07-31):** se usa **Gmail SMTP con App Password** (tenemos Google Workspace
> con el dominio `webiados.com`, así que no cuesta nada nuevo). **Resend queda anotado como el
> paso siguiente** si el volumen crece o si Gmail nos limita — ver "Plan B" abajo.
>
> Contexto: el envío al cliente (`POST /api/admin/quotes/{id}/send`) falló en producción con
> `535-5.7.8 BadCredentials` porque `MAIL_PASSWORD` no era una App Password válida. El código
> está bien; era configuración.

## Para qué se usa

- `sendQuoteToClient` — le manda al **cliente** el enlace de la landing + su clave. Es síncrono
  y propaga el error a propósito: si el correo no sale, la cotización **no** queda marcada como
  `SENT` (no miente). Por eso un SMTP mal configurado se ve como un 500 en «Enviar por correo».
- `notifySelection` — aviso interno a `NOTIFY_TO` cuando un cliente elige. Es `@Async` y sus
  fallas se registran y se tragan (no rompen la selección del cliente).

## Configuración vigente (Gmail) — variables de Railway

| Variable | Valor | Notas |
|---|---|---|
| `MAIL_HOST` | `smtp.gmail.com` | |
| `MAIL_PORT` | `587` | STARTTLS |
| `MAIL_SMTP_AUTH` | `true` | |
| `MAIL_SMTP_STARTTLS` | `true` | |
| `MAIL_USERNAME` | buzón **real** del Workspace que autentica | la cuenta dueña de la App Password |
| `MAIL_PASSWORD` | **App Password** de 16 caracteres, **sin espacios** | NO la contraseña normal |
| `MAIL_FROM` | igual a `MAIL_USERNAME`, o un **alias real** de esa cuenta | ver regla del From |

**Las credenciales las carga Felipe en Railway. El modelo no las recibe ni las escribe.**

### Regla del `MAIL_FROM`

Gmail SMTP solo deja enviar *como* una dirección que la cuenta autenticada tiene permitida: su
propia dirección, un **alias del usuario en Workspace**, o un **"Enviar como" verificado**. Un
`MAIL_FROM` que no sea la cuenta ni un alias suyo se **reescribe** al remitente autenticado (o se
rechaza). Lo simple y seguro: que `cotizaciones@webiados.com` sea un **buzón real** y poner
`MAIL_USERNAME` = `MAIL_FROM` = esa dirección.

### Requisitos de la App Password
1. La cuenta necesita **Verificación en 2 pasos** activada (si no, no aparece "Contraseñas de
   aplicaciones" en `myaccount.google.com` → Seguridad).
2. El admin del Workspace no debe tener bloqueadas las App Passwords (permitidas por defecto con 2SV).

### Cómo probar
Crear una cotización de prueba **con tu propio correo** como cliente y usar «Enviar por correo».
Debe llegar el mail con el enlace + la clave, y la cotización pasar a «Enviada».

## Plan B — Resend (si crece el volumen o Gmail limita)

Gmail Workspace tope ~2.000 destinatarios/día y no es un proveedor transaccional. Para migrar:
`MAIL_HOST=smtp.resend.com`, `MAIL_PORT=587`, `MAIL_USERNAME=resend`, `MAIL_PASSWORD`=API key de
Resend, `MAIL_FROM=cotizaciones@webiados.com`, y **verificar `webiados.com` en Resend** (registros
DNS). No hay cambio de código: el servicio lee todo de variables de entorno.
