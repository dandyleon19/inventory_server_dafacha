-- Stage 1: product catalog (no stock yet; stock per warehouse arrives with warehouses).

CREATE TABLE product_categories
(
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_product_categories_company_name ON product_categories (company_id, LOWER(name));

CREATE TABLE products
(
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT         NOT NULL,
    category_id BIGINT,
    name        VARCHAR(150)   NOT NULL,
    description TEXT,
    sku         VARCHAR(50)    NOT NULL,
    barcode     VARCHAR(50),
    unit        VARCHAR(20)    NOT NULL DEFAULT 'UNIT',
    cost_price  NUMERIC(12, 4) NOT NULL DEFAULT 0,
    sale_price  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    min_stock   NUMERIC(12, 3) NOT NULL DEFAULT 0,
    is_active   BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES product_categories (id),
    CONSTRAINT chk_product_cost_price CHECK (cost_price >= 0),
    CONSTRAINT chk_product_sale_price CHECK (sale_price >= 0),
    CONSTRAINT chk_product_min_stock CHECK (min_stock >= 0)
);

-- SKU is unique per company, ignoring case; barcode is optional but unique per company when present.
CREATE UNIQUE INDEX uq_products_company_sku ON products (company_id, LOWER(sku));
CREATE UNIQUE INDEX uq_products_company_barcode ON products (company_id, barcode) WHERE barcode IS NOT NULL;
CREATE INDEX idx_products_company_category ON products (company_id, category_id);
CREATE INDEX idx_products_company_name ON products (company_id, name);
