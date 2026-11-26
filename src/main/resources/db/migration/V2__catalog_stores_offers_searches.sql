-- V2: Catálogo (B: 6), Tiendas y Conectores (C: 4 restantes), Búsquedas (E: 3), Ofertas e Ingesta (D: 8)

-- ============================================================================
-- B. Catálogo (6 tablas)
-- ============================================================================

CREATE TABLE brands (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE categories (
    id        BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    name      VARCHAR(120) NOT NULL
);

CREATE INDEX idx_categories_parent ON categories (parent_id);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    brand_id    BIGINT REFERENCES brands(id) ON DELETE SET NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    name        VARCHAR(255) NOT NULL,
    model       VARCHAR(140),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_name_trgm ON products USING GIN (name gin_trgm_ops);
CREATE INDEX idx_products_category ON products (category_id);

CREATE TABLE product_attributes (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    value      VARCHAR(500) NOT NULL,
    CONSTRAINT uq_product_attr UNIQUE (product_id, name)
);

CREATE TABLE product_identifiers (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    type       VARCHAR(20) NOT NULL CHECK (type IN ('GTIN', 'EAN', 'UPC', 'ISBN', 'MPN')),
    value      VARCHAR(80) NOT NULL,
    CONSTRAINT uq_product_identifier UNIQUE (type, value)
);

CREATE INDEX idx_product_identifiers_product ON product_identifiers (product_id);

CREATE TABLE product_images (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url        VARCHAR(1000) NOT NULL,
    position   INT NOT NULL DEFAULT 0
);

-- ============================================================================
-- C. Tiendas y conectores (4 tablas restantes; countries creada en V1)
-- ============================================================================

CREATE TABLE stores (
    id                   BIGSERIAL PRIMARY KEY,
    code                 VARCHAR(50) NOT NULL UNIQUE,
    country_code         CHAR(2) NOT NULL REFERENCES countries(code),
    name                 VARCHAR(120) NOT NULL,
    base_url             VARCHAR(500) NOT NULL,
    default_currency     CHAR(3) NOT NULL REFERENCES currencies(code),
    reliability_score    NUMERIC(4,2) NOT NULL DEFAULT 0.80 CHECK (reliability_score >= 0 AND reliability_score <= 1),
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    data_kind            VARCHAR(10) NOT NULL DEFAULT 'DEMO' CHECK (data_kind IN ('REAL', 'DEMO')),
    shipping_policy_type VARCHAR(40) NOT NULL DEFAULT 'UNKNOWN',
    flat_shipping_amount NUMERIC(14,2),
    tax_included         BOOLEAN NOT NULL DEFAULT TRUE,
    tax_rate             NUMERIC(5,2)
);

CREATE TABLE store_connectors (
    id                   BIGSERIAL PRIMARY KEY,
    store_id             BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    connector_type       VARCHAR(120) NOT NULL,
    phase                SMALLINT NOT NULL DEFAULT 1,
    enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit_per_min   INT NOT NULL DEFAULT 20 CHECK (rate_limit_per_min > 0),
    credentials_ref      VARCHAR(150),
    kind                 VARCHAR(20) NOT NULL CHECK (kind IN ('API', 'SCRAPER', 'FEED', 'FALLBACK')),
    priority             INT NOT NULL DEFAULT 10,
    success_rate         NUMERIC(5,4) NOT NULL DEFAULT 1.0000 CHECK (success_rate >= 0 AND success_rate <= 1),
    consecutive_failures INT NOT NULL DEFAULT 0,
    last_success_at      TIMESTAMPTZ,
    circuit_state        VARCHAR(15) NOT NULL DEFAULT 'CLOSED' CHECK (circuit_state IN ('CLOSED', 'OPEN', 'HALF_OPEN')),
    config_json          JSONB NOT NULL DEFAULT '{}'::jsonb,
    evidence_url         VARCHAR(500),
    robots_checked_at    TIMESTAMPTZ,
    ttl_minutes          INT NOT NULL DEFAULT 1440,
    daily_call_budget    INT NOT NULL DEFAULT 500
);

CREATE INDEX idx_store_connectors_store_prio ON store_connectors (store_id, priority);

-- ============================================================================
-- E. Búsquedas (creamos searches antes de connector_executions por FK)
-- ============================================================================

CREATE TABLE searches (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    query_text  VARCHAR(300) NOT NULL,
    search_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' CHECK (search_type IN ('TEXT', 'BARCODE', 'IMAGE')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE search_filters (
    id        BIGSERIAL PRIMARY KEY,
    search_id BIGINT NOT NULL REFERENCES searches(id) ON DELETE CASCADE,
    name      VARCHAR(80) NOT NULL,
    value     VARCHAR(255) NOT NULL
);

CREATE TABLE connector_executions (
    id               BIGSERIAL PRIMARY KEY,
    connector_id     BIGINT NOT NULL REFERENCES store_connectors(id) ON DELETE CASCADE,
    search_id        BIGINT REFERENCES searches(id) ON DELETE SET NULL,
    started_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at      TIMESTAMPTZ,
    status           VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCESS', 'PARTIAL', 'FAILED')),
    items_fetched    INT NOT NULL DEFAULT 0,
    error_count      INT NOT NULL DEFAULT 0,
    trigger          VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (trigger IN ('SCHEDULED', 'MANUAL', 'LIVE')),
    runner           VARCHAR(20) NOT NULL DEFAULT 'LOCAL' CHECK (runner IN ('ACTIONS', 'RENDER', 'LOCAL')),
    items_created    INT NOT NULL DEFAULT 0,
    items_updated    INT NOT NULL DEFAULT 0,
    items_skipped    INT NOT NULL DEFAULT 0,
    completeness_pct NUMERIC(5,2),
    error_message    TEXT
);

CREATE INDEX idx_connector_executions_conn_date ON connector_executions (connector_id, started_at DESC);

CREATE TABLE connector_logs (
    id           BIGSERIAL PRIMARY KEY,
    execution_id BIGINT NOT NULL REFERENCES connector_executions(id) ON DELETE CASCADE,
    level        VARCHAR(15) NOT NULL,
    message      TEXT NOT NULL,
    logged_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- D. Ofertas, precios e ingesta (8 tablas)
-- ============================================================================

CREATE TABLE store_products (
    id                    BIGSERIAL PRIMARY KEY,
    store_id              BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id            BIGINT REFERENCES products(id) ON DELETE SET NULL,
    similar_to_product_id BIGINT REFERENCES products(id) ON DELETE SET NULL,
    external_id           VARCHAR(160) NOT NULL,
    title                 VARCHAR(350) NOT NULL,
    url                   VARCHAR(1000) NOT NULL,
    match_level           SMALLINT,
    match_confidence      NUMERIC(5,4),
    match_method          VARCHAR(40),
    brand_raw             VARCHAR(120),
    model_raw             VARCHAR(140),
    gtin_raw              VARCHAR(60),
    first_seen_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_store_external UNIQUE (store_id, external_id)
);

CREATE INDEX idx_store_products_product ON store_products (product_id);
CREATE INDEX idx_store_products_title_trgm ON store_products USING GIN (title gin_trgm_ops);

CREATE TABLE search_results (
    search_id        BIGINT NOT NULL REFERENCES searches(id) ON DELETE CASCADE,
    store_product_id BIGINT NOT NULL REFERENCES store_products(id) ON DELETE CASCADE,
    rank             INT NOT NULL,
    score            NUMERIC(8,4),
    PRIMARY KEY (search_id, store_product_id)
);

CREATE TABLE offers (
    id               BIGSERIAL PRIMARY KEY,
    store_product_id BIGINT NOT NULL REFERENCES store_products(id) ON DELETE CASCADE,
    execution_id     BIGINT REFERENCES connector_executions(id) ON DELETE SET NULL,
    price_amount     NUMERIC(14,2) NOT NULL CHECK (price_amount >= 0),
    currency_code    CHAR(3) NOT NULL REFERENCES currencies(code),
    shipping_amount  NUMERIC(14,2) CHECK (shipping_amount IS NULL OR shipping_amount >= 0),
    shipping_known   BOOLEAN NOT NULL DEFAULT FALSE,
    tax_amount       NUMERIC(14,2) CHECK (tax_amount IS NULL OR tax_amount >= 0),
    tax_known        BOOLEAN NOT NULL DEFAULT FALSE,
    final_price_ref  NUMERIC(14,2) NOT NULL CHECK (final_price_ref >= 0),
    exchange_rate_id BIGINT REFERENCES exchange_rates(id) ON DELETE SET NULL,
    availability     VARCHAR(40) NOT NULL DEFAULT 'IN_STOCK',
    source_url       VARCHAR(1000),
    checked_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_current       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uq_offers_one_current_per_sp
    ON offers (store_product_id) WHERE is_current = TRUE;
CREATE INDEX idx_offers_sp_checked ON offers (store_product_id, checked_at DESC);

CREATE TABLE price_history (
    id               BIGSERIAL PRIMARY KEY,
    offer_id         BIGINT NOT NULL REFERENCES offers(id) ON DELETE CASCADE,
    store_product_id BIGINT NOT NULL REFERENCES store_products(id) ON DELETE CASCADE,
    price            NUMERIC(14,2) NOT NULL,
    currency         CHAR(3) NOT NULL REFERENCES currencies(code),
    shipping         NUMERIC(14,2),
    final_price_ref  NUMERIC(14,2) NOT NULL,
    exchange_rate_id BIGINT REFERENCES exchange_rates(id) ON DELETE SET NULL,
    availability     VARCHAR(40) NOT NULL DEFAULT 'IN_STOCK',
    recorded_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_price_history_sp_date ON price_history (store_product_id, recorded_at DESC);

CREATE TABLE shipping_options (
    id                  BIGSERIAL PRIMARY KEY,
    store_id            BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    destination_country CHAR(2) NOT NULL REFERENCES countries(code),
    cost                NUMERIC(14,2),
    currency            CHAR(3) NOT NULL REFERENCES currencies(code),
    min_days            INT,
    max_days            INT
);

CREATE TABLE raw_offers (
    id               BIGSERIAL PRIMARY KEY,
    execution_id     BIGINT NOT NULL REFERENCES connector_executions(id) ON DELETE CASCADE,
    store_product_id BIGINT REFERENCES store_products(id) ON DELETE SET NULL,
    payload_json     JSONB NOT NULL,
    parse_status     VARCHAR(15) NOT NULL CHECK (parse_status IN ('OK', 'PARTIAL', 'FAILED')),
    fetched_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE store_product_attributes (
    id               BIGSERIAL PRIMARY KEY,
    store_product_id BIGINT NOT NULL REFERENCES store_products(id) ON DELETE CASCADE,
    name             VARCHAR(100) NOT NULL,
    value            VARCHAR(500) NOT NULL
);

CREATE TABLE store_product_images (
    id               BIGSERIAL PRIMARY KEY,
    store_product_id BIGINT NOT NULL REFERENCES store_products(id) ON DELETE CASCADE,
    url              VARCHAR(1000) NOT NULL,
    position         INT NOT NULL DEFAULT 0,
    source           VARCHAR(60) NOT NULL DEFAULT 'CONNECTOR',
    checked_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE data_quality_issues (
    id               BIGSERIAL PRIMARY KEY,
    execution_id     BIGINT NOT NULL REFERENCES connector_executions(id) ON DELETE CASCADE,
    store_product_id BIGINT REFERENCES store_products(id) ON DELETE SET NULL,
    field            VARCHAR(80) NOT NULL,
    issue_type       VARCHAR(20) NOT NULL CHECK (issue_type IN ('MISSING', 'INVALID', 'OUTLIER')),
    resolved         BOOLEAN NOT NULL DEFAULT FALSE
);
