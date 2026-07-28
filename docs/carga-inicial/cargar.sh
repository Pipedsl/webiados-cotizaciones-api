#!/usr/bin/env bash
#
# Carga las dos cotizaciones reales del histórico (Sprint 1, tareas 1.2 y 1.3).
#
#   Macarena Larraín          → SELECTED (aceptó la Opción C, $380.000 + IVA)
#   Pastelería Vientos del Sur → SENT    (enviada, esperando respuesta)
#
# Ambas quedan con su fecha real de emisión y de envío, y NINGUNA le manda un correo al
# cliente: ya recibieron su cotización en PDF hace semanas. Para eso se usa `mark-sent`,
# que registra una entrega hecha fuera del sistema, en vez de `send`, que sí envía.
#
# USO
#   export ADMIN_EMAIL='felipe@webiados.com'
#   export ADMIN_PASSWORD='...'          # el de Railway (ADMIN_BOOTSTRAP_PASSWORD)
#   ./cargar.sh
#
# Variables opcionales:
#   API      base del servicio (default: el Railway de producción)
#   DRY_RUN=1  muestra lo que haría, sin escribir nada
#
# NO es idempotente: si lo corres dos veces, crea dos veces. Revisa el panel antes de
# repetir. Escribe en la base de producción.

set -euo pipefail

API="${API:-https://cotizaciones-api-production-e0fb.up.railway.app}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

: "${ADMIN_EMAIL:?falta ADMIN_EMAIL}"
: "${ADMIN_PASSWORD:?falta ADMIN_PASSWORD}"

jqf() { python3 -c "import sys,json; print(json.load(sys.stdin)$1)"; }

echo "→ API: $API"

if [ "${DRY_RUN:-0}" = "1" ]; then
  echo "   (DRY_RUN: no se escribe nada)"
  for f in macarena.json vientos-del-sur.json; do
    python3 - "$DIR/$f" <<'PY'
import json, sys
d = json.load(open(sys.argv[1]))
print(f"\n  {d['clientName']}  —  emitida {d['createdAt']}")
for o in d['options']:
    m = f" + ${o['precioMensual']:,}/mes".replace(',', '.') if o.get('precioMensual') else ""
    print(f"    · {o['titulo']}: ${o['precio']:,}".replace(',', '.') + m)
PY
  done
  exit 0
fi

echo "→ Login admin…"
TOKEN=$(curl -sS -X POST "$API/api/admin/auth/login" \
  -H 'Content-Type: application/json' \
  -d "$(python3 -c 'import json,os; print(json.dumps({"email":os.environ["ADMIN_EMAIL"],"password":os.environ["ADMIN_PASSWORD"]}))')" \
  | jqf '["token"]')
[ -n "$TOKEN" ] || { echo "✗ no se obtuvo token"; exit 1; }
echo "  ok"

api() {
  local method="$1" path="$2"; shift 2
  curl -sS -X "$method" "$API$path" \
    -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' "$@"
}

# --- 1. Macarena: creada, entregada y aceptada (Opción C) ---------------------
echo "→ Macarena Larraín…"
MACA=$(api POST /api/admin/quotes --data-binary "@$DIR/macarena.json")
MACA_ID=$(echo "$MACA"     | jqf '["id"]')
MACA_CODIGO=$(echo "$MACA" | jqf '["codigo"]')
MACA_CLAVE=$(echo "$MACA"  | jqf '["clave"]')
echo "  creada · código $MACA_CODIGO · clave $MACA_CLAVE"

# Se entregó el 24 JUL 2026 en PDF, fuera del sistema. No se le manda correo.
api POST "/api/admin/quotes/$MACA_ID/mark-sent" \
  -d '{"sentAt":"2026-07-24T15:00:00Z"}' > /dev/null
echo "  marcada como entregada (2026-07-24), sin enviar correo"

# Aceptó la Opción C: se registra como la eligió el cliente, para que quede en la
# bitácora de selecciones igual que una elección hecha desde la landing.
MACA_OPT_C=$(api GET "/api/admin/quotes/$MACA_ID" | jqf '["options"][2]["id"]')
CTOKEN=$(curl -sS -X POST "$API/api/client/quotes/$MACA_CODIGO/unlock" \
  -H 'Content-Type: application/json' \
  -d "$(python3 -c "import json; print(json.dumps({'clave':'$MACA_CLAVE'}))")" | jqf '["token"]')
curl -sS -X POST "$API/api/client/quotes/$MACA_CODIGO/select" \
  -H "Authorization: Bearer $CTOKEN" -H 'Content-Type: application/json' \
  -d "$(python3 -c "import json; print(json.dumps({'optionId':'$MACA_OPT_C'}))")" > /dev/null
echo "  Opción C registrada como elegida → SELECTED"
echo "  ⚠ esto dispara el aviso interno a NOTIFY_TO (contacto@webiados.com)"

# --- 2. Vientos del Sur: creada y enviada, esperando respuesta -----------------
echo "→ Pastelería Vientos del Sur…"
VDS=$(api POST /api/admin/quotes --data-binary "@$DIR/vientos-del-sur.json")
VDS_ID=$(echo "$VDS"     | jqf '["id"]')
VDS_CODIGO=$(echo "$VDS" | jqf '["codigo"]')
VDS_CLAVE=$(echo "$VDS"  | jqf '["clave"]')
echo "  creada · código $VDS_CODIGO · clave $VDS_CLAVE"

api POST "/api/admin/quotes/$VDS_ID/mark-sent" \
  -d '{"sentAt":"2026-07-27T12:00:00Z"}' > /dev/null
echo "  marcada como enviada (2026-07-27) → SENT"

# --- Verificación -------------------------------------------------------------
echo
echo "→ Estado final:"
api GET /api/admin/quotes | python3 -c '
import sys, json
for q in json.load(sys.stdin):
    print(f"   {q[\"status\"]:<9} {q[\"clientName\"]:<28} enviada: {q[\"sentAt\"] or \"—\"}")
'

cat <<NOTA

────────────────────────────────────────────────────────────────────────────
GUARDA ESTO — la clave en texto se muestra una sola vez acá (aunque también
queda en clave_texto, visible en el detalle del panel):

  Macarena Larraín           $MACA_CODIGO / $MACA_CLAVE
  Pastelería Vientos del Sur $VDS_CODIGO / $VDS_CLAVE

Verificación (tarea 1.2 y 1.3): entra al panel en webiados.com/admin y
confirma que aparecen las dos, Macarena en SELECTED y la pastelería en SENT.
────────────────────────────────────────────────────────────────────────────
NOTA
