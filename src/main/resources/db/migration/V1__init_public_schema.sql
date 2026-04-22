-- ============================================
-- V1: PUBLIC SCHEMA (UPDATED)
-- ============================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- USERS (GLOBAL IDENTITY)
-- =========================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255),

    password_hash TEXT NOT NULL,

    is_active BOOLEAN DEFAULT TRUE,
    last_login    TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
    );

CREATE UNIQUE INDEX IF NOT EXISTS unique_email
    ON users(email)
    WHERE email IS NOT NULL;

-- =========================
-- TENANTS (BUSINESS)
-- =========================
CREATE TABLE IF NOT EXISTS tenants (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(255) NOT NULL,

    slug VARCHAR(255) NOT NULL UNIQUE,

    gstin VARCHAR(50),

    db_schema_name VARCHAR(100) NOT NULL UNIQUE,

    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMPTZ DEFAULT NOW()
    );

CREATE UNIQUE INDEX IF NOT EXISTS unique_gstin
    ON tenants(gstin)
    WHERE gstin IS NOT NULL;

-- =========================
-- USER ↔ TENANT MAPPING
-- =========================
CREATE TABLE IF NOT EXISTS user_tenants (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,

    role VARCHAR(50) NOT NULL,

    created_at TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE(user_id, tenant_id),

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
    );