-- V1: Extensiones PostgreSQL, Internacionalización (G), Usuarios y Seguridad (A), Administración (H)
-- Motor: PostgreSQL 17 + pgvector (Supabase / Docker local)

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- G. Internacionalización (4 tablas) + countries (de C, requerido por FK)
-- ============================================================================

CREATE TABLE currencies (
    code        CHAR(3) PRIMARY KEY,
    name        VARCHAR(80) NOT NULL,
    decimals    SMALLINT NOT NULL DEFAULT 2 CHECK (decimals >= 0 AND decimals <= 4)
);

CREATE TABLE languages (
    code        VARCHAR(10) PRIMARY KEY,
    name        VARCHAR(80) NOT NULL
);

CREATE TABLE countries (
    code             CHAR(2) PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    default_currency CHAR(3) NOT NULL REFERENCES currencies(code)
);

CREATE TABLE country_settings (
    country_code     CHAR(2) PRIMARY KEY REFERENCES countries(code) ON DELETE CASCADE,
    default_currency CHAR(3) NOT NULL REFERENCES currencies(code),
    default_language VARCHAR(10) NOT NULL REFERENCES languages(code),
    import_tax_pct   NUMERIC(5,2) NOT NULL DEFAULT 0.00 CHECK (import_tax_pct >= 0),
    timezone         VARCHAR(64) NOT NULL DEFAULT 'America/Bogota'
);

CREATE TABLE exchange_rates (
    id             BIGSERIAL PRIMARY KEY,
    base_currency  CHAR(3) NOT NULL REFERENCES currencies(code),
    quote_currency CHAR(3) NOT NULL REFERENCES currencies(code),
    rate           NUMERIC(18,6) NOT NULL CHECK (rate > 0),
    source         VARCHAR(100) NOT NULL,
    fetched_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_exchange_diff_currency CHECK (base_currency <> quote_currency)
);

CREATE INDEX idx_exchange_rates_pair_date
    ON exchange_rates (base_currency, quote_currency, fetched_at DESC);

-- ============================================================================
-- A. Usuarios y seguridad (5 tablas)
-- ============================================================================

CREATE TABLE roles (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(120) NOT NULL,
    country_code  CHAR(2) NOT NULL DEFAULT 'CO' REFERENCES countries(code),
    locale        VARCHAR(10) NOT NULL DEFAULT 'es-CO' REFERENCES languages(code),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE user_preferences (
    user_id       BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    currency_code CHAR(3) NOT NULL DEFAULT 'COP' REFERENCES currencies(code),
    locale        VARCHAR(10) NOT NULL DEFAULT 'es-CO' REFERENCES languages(code)
);

CREATE TABLE sessions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    BIGINT REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_sessions_user ON sessions (user_id);

-- ============================================================================
-- H. Administración (3 tablas)
-- ============================================================================

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(100) NOT NULL,
    detail_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);

CREATE TABLE system_settings (
    key        VARCHAR(120) PRIMARY KEY,
    value      TEXT NOT NULL,
    updated_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE system_metrics (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    value       NUMERIC(18,4) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_system_metrics_name_date ON system_metrics (name, recorded_at DESC);
