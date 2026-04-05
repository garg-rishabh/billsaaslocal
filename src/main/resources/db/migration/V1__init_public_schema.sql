-- Enable UUID generation
CREATE
EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- TENANTS TABLE (PUBLIC)
-- =========================
CREATE TABLE tenants
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name           VARCHAR(255) NOT NULL,
    slug           VARCHAR(100) NOT NULL UNIQUE,

    gstin          VARCHAR(50),
    phone          VARCHAR(20),
    email          VARCHAR(255),

    plan_type      VARCHAR(50)      DEFAULT 'FREE',

    db_schema_name VARCHAR(100) NOT NULL UNIQUE,

    is_active      BOOLEAN          DEFAULT TRUE,

    created_at     TIMESTAMPTZ      DEFAULT NOW()
);

-- Index for fast lookup
CREATE INDEX idx_tenants_slug ON tenants (slug);


-- =========================
-- USERS TABLE (PUBLIC)
-- =========================
CREATE TABLE users
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    tenant_id     UUID         NOT NULL,

    email         VARCHAR(255) NOT NULL,
    password_hash TEXT         NOT NULL,

    full_name     VARCHAR(255),

    role          VARCHAR(50)      DEFAULT 'USER',

    is_active     BOOLEAN          DEFAULT TRUE,

    last_login    TIMESTAMPTZ,

    created_at    TIMESTAMPTZ      DEFAULT NOW(),

    CONSTRAINT fk_users_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES tenants (id)
            ON DELETE CASCADE
);

-- UNIQUE constraint per tenant
ALTER TABLE users
    ADD CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email);

-- Index for fast queries
CREATE INDEX idx_users_tenant_email ON users (tenant_id, email);