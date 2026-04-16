-- ============================================
-- V2: Tenant Schema Provisioning (Production)
-- ============================================

CREATE OR REPLACE FUNCTION provision_tenant_schema(schema_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN

    -- =========================
    -- CREATE SCHEMA
    -- =========================
EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', schema_name);

-- =========================
-- CATEGORIES
-- =========================
EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.categories (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            name VARCHAR(255) NOT NULL,
            description TEXT,
            is_active BOOLEAN DEFAULT TRUE,
            created_at TIMESTAMPTZ DEFAULT NOW(),
            updated_at TIMESTAMPTZ DEFAULT NOW()
        )', schema_name);

-- =========================
-- SUPPLIERS
-- =========================
EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.suppliers (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            name VARCHAR(255) NOT NULL,
            contact_person VARCHAR(255),
            phone VARCHAR(20),
            email VARCHAR(255),
            address TEXT,
            gstin VARCHAR(50),
            is_active BOOLEAN DEFAULT TRUE,
            created_at TIMESTAMPTZ DEFAULT NOW(),
            updated_at TIMESTAMPTZ DEFAULT NOW()
        )', schema_name);

-- =========================
-- INVENTORY ITEMS
-- =========================
EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.inventory_items (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            name VARCHAR(255) NOT NULL,
            sku VARCHAR(100) UNIQUE,
            barcode VARCHAR(100),

            category_id UUID,
            supplier_id UUID,

            unit VARCHAR(20),
            cost_price NUMERIC(10,2),
            selling_price NUMERIC(10,2),

            quantity INT DEFAULT 0,
            reorder_level INT DEFAULT 0,
            reorder_quantity INT DEFAULT 0,

            is_active BOOLEAN DEFAULT TRUE,

            created_at TIMESTAMPTZ DEFAULT NOW(),
            updated_at TIMESTAMPTZ DEFAULT NOW()
        )', schema_name);

-- =========================
-- STOCK MOVEMENTS
-- =========================
EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.stock_movements (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            item_id UUID NOT NULL,

            movement_type VARCHAR(20) NOT NULL,
            quantity INT NOT NULL,

            reference_type VARCHAR(50),
            reference_id UUID,

            notes TEXT,

            created_at TIMESTAMPTZ DEFAULT NOW()
        )', schema_name);

-- =========================
-- CUSTOMERS
-- =========================
EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.customers (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            name VARCHAR(255) NOT NULL,
            phone VARCHAR(20),
            email VARCHAR(255),
            address TEXT,
            gstin VARCHAR(50),

            credit_limit NUMERIC(12,2) DEFAULT 0,
            outstanding_balance NUMERIC(12,2) DEFAULT 0,

            is_active BOOLEAN DEFAULT TRUE,

            created_at TIMESTAMPTZ DEFAULT NOW(),
            updated_at TIMESTAMPTZ DEFAULT NOW()
        )', schema_name);

-- =========================
-- INVOICES
-- =========================
EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.invoices (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

            invoice_number VARCHAR(100) UNIQUE NOT NULL,

            customer_id UUID,

            subtotal NUMERIC(12,2),
            tax_amount NUMERIC(12,2),
            discount_amount NUMERIC(12,2),
            total_amount NUMERIC(12,2),

            status VARCHAR(50) DEFAULT ''DRAFT'',

            invoice_date TIMESTAMPTZ DEFAULT NOW(),
            due_date TIMESTAMPTZ,

            notes TEXT,

            created_at TIMESTAMPTZ DEFAULT NOW(),
            updated_at TIMESTAMPTZ DEFAULT NOW()
        )', schema_name);

-- =========================
-- INVOICE LINE ITEMS
-- =========================
EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.invoice_line_items (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

            invoice_id UUID NOT NULL,
            item_id UUID NOT NULL,

            quantity INT NOT NULL,
            unit_price NUMERIC(10,2),
            discount NUMERIC(10,2),
            tax NUMERIC(10,2),

            total NUMERIC(12,2)
        )', schema_name);

-- =========================
-- PAYMENTS
-- =========================
EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.payments (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

            invoice_id UUID NOT NULL,

            amount NUMERIC(12,2) NOT NULL,

            payment_mode VARCHAR(50),
            transaction_ref VARCHAR(255),

            payment_date TIMESTAMPTZ DEFAULT NOW(),

            notes TEXT,

            created_at TIMESTAMPTZ DEFAULT NOW()
        )', schema_name);

-- =========================
-- INDEXES
-- =========================
EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_items_name ON %I.inventory_items(name)', schema_name, schema_name);
EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_items_sku ON %I.inventory_items(sku)', schema_name, schema_name);
EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_customers_phone ON %I.customers(phone)', schema_name, schema_name);
EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_invoice_number ON %I.invoices(invoice_number)', schema_name, schema_name);

-- =========================
-- FOREIGN KEYS (FINAL CORRECT)
-- =========================

-- inventory_items → categories
BEGIN
EXECUTE format('
        ALTER TABLE %I.inventory_items
        ADD CONSTRAINT fk_inventory_category
        FOREIGN KEY (category_id) REFERENCES %I.categories(id)
    ', schema_name, schema_name);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END;

-- inventory_items → suppliers
BEGIN
EXECUTE format('
        ALTER TABLE %I.inventory_items
        ADD CONSTRAINT fk_inventory_supplier
        FOREIGN KEY (supplier_id) REFERENCES %I.suppliers(id)
    ', schema_name, schema_name);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END;

-- stock_movements → inventory_items
BEGIN
EXECUTE format('
        ALTER TABLE %I.stock_movements
        ADD CONSTRAINT fk_stock_item
        FOREIGN KEY (item_id) REFERENCES %I.inventory_items(id)
    ', schema_name, schema_name);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END;

-- invoices → customers
BEGIN
EXECUTE format('
        ALTER TABLE %I.invoices
        ADD CONSTRAINT fk_invoice_customer
        FOREIGN KEY (customer_id) REFERENCES %I.customers(id)
    ', schema_name, schema_name);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END;

-- invoice_line_items → invoices
BEGIN
EXECUTE format('
        ALTER TABLE %I.invoice_line_items
        ADD CONSTRAINT fk_line_invoice
        FOREIGN KEY (invoice_id) REFERENCES %I.invoices(id)
    ', schema_name, schema_name);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END;

-- invoice_line_items → inventory_items
BEGIN
EXECUTE format('
        ALTER TABLE %I.invoice_line_items
        ADD CONSTRAINT fk_line_item
        FOREIGN KEY (item_id) REFERENCES %I.inventory_items(id)
    ', schema_name, schema_name);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END;

-- payments → invoices
BEGIN
EXECUTE format('
        ALTER TABLE %I.payments
        ADD CONSTRAINT fk_payment_invoice
        FOREIGN KEY (invoice_id) REFERENCES %I.invoices(id)
    ', schema_name, schema_name);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END;

END;
$$;