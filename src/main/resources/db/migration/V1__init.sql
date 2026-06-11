-- Webiados cotizaciones — esquema inicial

CREATE TABLE admin_user (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE quote (
    id                 UUID PRIMARY KEY,
    codigo             VARCHAR(32)  NOT NULL UNIQUE,
    clave_hash         VARCHAR(255) NOT NULL,
    client_name        VARCHAR(255) NOT NULL,
    client_email       VARCHAR(255),
    notes              TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at         TIMESTAMPTZ  NOT NULL,
    selected_option_id UUID,
    selected_at        TIMESTAMPTZ
);

CREATE INDEX idx_quote_codigo ON quote (codigo);

CREATE TABLE quote_option (
    id          UUID PRIMARY KEY,
    quote_id    UUID NOT NULL REFERENCES quote (id) ON DELETE CASCADE,
    order_index INT  NOT NULL,
    titulo      VARCHAR(255)   NOT NULL,
    descripcion TEXT,
    precio      NUMERIC(14, 2) NOT NULL,
    currency    VARCHAR(3)     NOT NULL DEFAULT 'CLP',
    recomendado BOOLEAN        NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_quote_option_quote ON quote_option (quote_id);

CREATE TABLE quote_option_feature (
    option_id UUID NOT NULL REFERENCES quote_option (id) ON DELETE CASCADE,
    position  INT  NOT NULL,
    feature   VARCHAR(500) NOT NULL,
    PRIMARY KEY (option_id, position)
);

CREATE TABLE selection (
    id         UUID PRIMARY KEY,
    quote_id   UUID NOT NULL REFERENCES quote (id) ON DELETE CASCADE,
    option_id  UUID NOT NULL REFERENCES quote_option (id) ON DELETE CASCADE,
    kind       VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_selection_quote ON selection (quote_id);

-- FK diferida del plan elegido (se valida a nivel app, no en SQL para evitar ciclos)
ALTER TABLE quote
    ADD CONSTRAINT fk_quote_selected_option
        FOREIGN KEY (selected_option_id) REFERENCES quote_option (id) ON DELETE SET NULL;
