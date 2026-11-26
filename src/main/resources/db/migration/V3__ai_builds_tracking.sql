-- V3: IA y Armador General (I: 15 tablas) + Seguimiento (F: 3 tablas)
-- Total acumulado entre V1 + V2 + V3 = 52 tablas.

-- ============================================================================
-- I. IA y Armador General (15 tablas)
-- ============================================================================

CREATE TABLE ai_conversations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        BIGINT REFERENCES users(id) ON DELETE SET NULL,
    session_key    VARCHAR(120) NOT NULL,
    provider       VARCHAR(50) NOT NULL,
    prompt_version VARCHAR(30) NOT NULL DEFAULT 'v1',
    consent_given  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMPTZ,
    expires_at     TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '30 days')
);

CREATE TABLE ai_messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT', 'TOOL')),
    content         TEXT NOT NULL,
    tokens          INT NOT NULL DEFAULT 0,
    latency_ms      INT NOT NULL DEFAULT 0,
    cost            NUMERIC(14,6) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_messages_conv ON ai_messages (conversation_id, created_at);

CREATE TABLE ai_tool_calls (
    id             BIGSERIAL PRIMARY KEY,
    message_id     BIGINT NOT NULL REFERENCES ai_messages(id) ON DELETE CASCADE,
    tool_name      VARCHAR(80) NOT NULL,
    arguments_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    success        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE ai_validation_results (
    id            BIGSERIAL PRIMARY KEY,
    message_id    BIGINT NOT NULL UNIQUE REFERENCES ai_messages(id) ON DELETE CASCADE,
    passed        BOOLEAN NOT NULL,
    issues_json   JSONB NOT NULL DEFAULT '[]'::jsonb,
    regenerations INT NOT NULL DEFAULT 0
);

CREATE TABLE ai_usage (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT REFERENCES users(id) ON DELETE SET NULL,
    session_key    VARCHAR(120) NOT NULL,
    usage_date     DATE NOT NULL DEFAULT CURRENT_DATE,
    requests       INT NOT NULL DEFAULT 0,
    tokens         INT NOT NULL DEFAULT 0,
    image_requests INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_ai_usage_date ON ai_usage (usage_date, user_id);

CREATE TABLE knowledge_documents (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code)
);

CREATE TABLE knowledge_chunks (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES knowledge_documents(id) ON DELETE CASCADE,
    model_id    VARCHAR(80) NOT NULL,
    content     TEXT NOT NULL,
    embedding   vector(768)
);

CREATE TABLE product_embeddings (
    id           BIGSERIAL PRIMARY KEY,
    product_id   BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    model_id     VARCHAR(80) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding    vector(768),
    CONSTRAINT uq_product_embedding_model UNIQUE (product_id, model_id)
);

CREATE TABLE image_queries (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id) ON DELETE SET NULL,
    search_id       BIGINT REFERENCES searches(id) ON DELETE SET NULL,
    conversation_id UUID REFERENCES ai_conversations(id) ON DELETE SET NULL,
    image_hash      VARCHAR(64) NOT NULL,
    barcode         VARCHAR(80),
    provider        VARCHAR(50),
    confidence      NUMERIC(5,4),
    result_json     JSONB NOT NULL DEFAULT '{}'::jsonb,
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '7 days')
);

-- Tablas de armado generalizado (cualquier categoría/setup/lista con presupuesto + compatibilidad)
CREATE TABLE item_specs (
    product_id  BIGINT PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    category    VARCHAR(80) NOT NULL DEFAULT 'GENERAL',
    socket      VARCHAR(60),
    memory_type VARCHAR(40),
    tdp         INT,
    length_mm   INT,
    height_mm   INT,
    form_factor VARCHAR(40),
    extra_json  JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE compat_rules (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(60) NOT NULL UNIQUE,
    category    VARCHAR(80) NOT NULL DEFAULT 'GENERAL',
    severity    VARCHAR(15) NOT NULL CHECK (severity IN ('ERROR', 'WARNING')),
    description TEXT NOT NULL
);

CREATE TABLE item_benchmarks (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    benchmark  VARCHAR(100) NOT NULL,
    score      NUMERIC(12,2) NOT NULL
);

CREATE TABLE builds (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID REFERENCES ai_conversations(id) ON DELETE SET NULL,
    name            VARCHAR(160) NOT NULL DEFAULT 'Mi configuración',
    category        VARCHAR(80) NOT NULL DEFAULT 'GENERAL',
    budget          NUMERIC(14,2) NOT NULL CHECK (budget >= 0),
    currency        CHAR(3) NOT NULL DEFAULT 'COP' REFERENCES currencies(code),
    purpose         VARCHAR(200),
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'VALID', 'INCOMPLETE', 'ARCHIVED')),
    total_final_ref NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE build_items (
    id            BIGSERIAL PRIMARY KEY,
    build_id      BIGINT NOT NULL REFERENCES builds(id) ON DELETE CASCADE,
    product_id    BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    offer_id      BIGINT REFERENCES offers(id) ON DELETE SET NULL,
    price_at_save NUMERIC(14,2) NOT NULL,
    quantity      INT NOT NULL DEFAULT 1 CHECK (quantity > 0)
);

CREATE TABLE build_validations (
    id       BIGSERIAL PRIMARY KEY,
    build_id BIGINT NOT NULL REFERENCES builds(id) ON DELETE CASCADE,
    rule_id  INT NOT NULL REFERENCES compat_rules(id) ON DELETE CASCADE,
    severity VARCHAR(15) NOT NULL CHECK (severity IN ('ERROR', 'WARNING')),
    passed   BOOLEAN NOT NULL,
    message  TEXT NOT NULL
);

-- ============================================================================
-- F. Seguimiento (3 tablas)
-- ============================================================================

CREATE TABLE favorites (
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, product_id)
);

CREATE TABLE price_alerts (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id   BIGINT REFERENCES products(id) ON DELETE CASCADE,
    build_id     BIGINT REFERENCES builds(id) ON DELETE CASCADE,
    target_price NUMERIC(14,2) NOT NULL CHECK (target_price > 0),
    currency     CHAR(3) NOT NULL DEFAULT 'COP' REFERENCES currencies(code),
    notified     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_alert_target CHECK (product_id IS NOT NULL OR build_id IS NOT NULL)
);

CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    alert_id   BIGINT REFERENCES price_alerts(id) ON DELETE SET NULL,
    message    VARCHAR(500) NOT NULL,
    read       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_unread ON notifications (user_id, read, created_at DESC);
