-- V4 — El estado de la cotización deja de ser derivado y pasa a guardarse, con su fecha
-- de envío. Sin esto no existe "enviada" y no se puede calcular una tasa de cierre.
--
-- Antes: status se calculaba en cada lectura desde (selected_option_id, expires_at) y
-- solo podía valer PENDING, SELECTED o EXPIRED. "Enviada" y "rechazada" no existían.
--
-- Después: `status` se persiste con PENDING | SENT | SELECTED | REJECTED.
-- EXPIRED se sigue derivando —es función del tiempo, no un hecho que alguien decide—
-- y se calcula sobre PENDING/SENT en la capa de dominio.
--
-- Compatibilidad: PENDING, SELECTED y EXPIRED conservan su nombre y significado, así que
-- el frontend que hoy los muestra no se rompe. SENT y REJECTED son agregados.

ALTER TABLE quote ADD COLUMN IF NOT EXISTS status      VARCHAR(16);
ALTER TABLE quote ADD COLUMN IF NOT EXISTS sent_at     TIMESTAMPTZ;
ALTER TABLE quote ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMPTZ;

-- Backfill de las cotizaciones que ya existen.
--
-- Solo se puede afirmar con certeza el caso SELECTED: si hay una opción elegida, el
-- cliente la eligió. Para el resto NO hay dato de envío en ninguna parte del esquema
-- anterior, así que se marcan PENDING. Es deliberado: preferimos subcontar los envíos
-- antes que inventar una fecha de envío que falsearía la tasa de cierre.
UPDATE quote
   SET status = CASE WHEN selected_option_id IS NOT NULL THEN 'SELECTED' ELSE 'PENDING' END
 WHERE status IS NULL;

-- selected_at ya existía y es la fecha real de la elección; no se toca.

ALTER TABLE quote ALTER COLUMN status SET NOT NULL;
ALTER TABLE quote ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE quote
    ADD CONSTRAINT chk_quote_status
        CHECK (status IN ('PENDING', 'SENT', 'SELECTED', 'REJECTED'));

-- Coherencia entre el estado y sus fechas: si está enviada tiene fecha de envío, si está
-- rechazada tiene fecha de rechazo. Esto es lo que protege la métrica del embudo.
ALTER TABLE quote
    ADD CONSTRAINT chk_quote_sent_at
        CHECK (status <> 'SENT' OR sent_at IS NOT NULL);

ALTER TABLE quote
    ADD CONSTRAINT chk_quote_rejected_at
        CHECK (status <> 'REJECTED' OR rejected_at IS NOT NULL);

-- Para contar "cuántas envié este mes" sin escanear la tabla entera.
CREATE INDEX IF NOT EXISTS idx_quote_status  ON quote (status);
CREATE INDEX IF NOT EXISTS idx_quote_sent_at ON quote (sent_at);

-- IVA de la cotización. Se guarda el porcentaje aplicado (no el monto) para que una
-- cotización histórica siga mostrando el IVA que tenía cuando se emitió, aunque la tasa
-- cambie. Los totales se calculan; no se almacenan montos redundantes.
ALTER TABLE quote ADD COLUMN IF NOT EXISTS iva_pct INT NOT NULL DEFAULT 19;

ALTER TABLE quote
    ADD CONSTRAINT chk_quote_iva_pct CHECK (iva_pct >= 0 AND iva_pct <= 100);

-- Precio recurrente por opción. Las dos cotizaciones reales lo tienen (la pastelería
-- cobra $49.000/mes en A y B y $74.000/mes en C) y hasta ahora solo se podía escribir
-- como texto libre, lo que lo dejaba fuera de cualquier cálculo.
-- NULL = esta opción no tiene mensualidad. 0 sería "mensualidad de cero", que es distinto.
ALTER TABLE quote_option ADD COLUMN IF NOT EXISTS precio_mensual NUMERIC(14, 2);

ALTER TABLE quote_option
    ADD CONSTRAINT chk_option_precio_mensual
        CHECK (precio_mensual IS NULL OR precio_mensual >= 0);
