-- Stage 2: warehouses, stock per warehouse, and the movement log (kardex).
-- Foreign keys use the default NO ACTION on purpose (not RESTRICT): it lets a company delete cascade
-- through all of its tables, while still blocking deletion of a single warehouse/product that has history.

CREATE TABLE warehouses
(
    id         BIGSERIAL PRIMARY KEY,
    company_id BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    address    VARCHAR(200),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_warehouse_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_warehouses_company_name ON warehouses (company_id, LOWER(name));

-- Current stock. Quantity may go negative only when the company allows it (company_settings), so there is
-- deliberately no CHECK (quantity >= 0): the guard lives in the atomic UPDATE used to apply movements.
CREATE TABLE stock_levels
(
    company_id   BIGINT         NOT NULL,
    warehouse_id BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,
    quantity     NUMERIC(14, 3) NOT NULL DEFAULT 0,
    updated_at   TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (warehouse_id, product_id),
    CONSTRAINT fk_stock_level_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_level_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_level_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_stock_levels_product ON stock_levels (product_id);
CREATE INDEX idx_stock_levels_company ON stock_levels (company_id);

-- Append-only log. `quantity` is signed (positive = stock in, negative = stock out); `balance_after` is the
-- stock of that product in that warehouse right after the movement; `unit_cost` is a snapshot of the
-- product's cost at that moment. The two legs of a transfer share `transfer_ref`.
CREATE TABLE stock_movements
(
    id            BIGSERIAL PRIMARY KEY,
    company_id    BIGINT         NOT NULL,
    warehouse_id  BIGINT         NOT NULL,
    product_id    BIGINT         NOT NULL,
    type          VARCHAR(20)    NOT NULL,
    quantity      NUMERIC(14, 3) NOT NULL,
    balance_after NUMERIC(14, 3) NOT NULL,
    unit_cost     NUMERIC(12, 4) NOT NULL DEFAULT 0,
    notes         TEXT,
    transfer_ref  VARCHAR(36),
    user_id       BIGINT,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_movement_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_movement_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_movement_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_movement_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_stock_movement_quantity CHECK (quantity <> 0)
);

CREATE INDEX idx_stock_movements_company_created ON stock_movements (company_id, created_at DESC);
CREATE INDEX idx_stock_movements_product_created ON stock_movements (product_id, created_at DESC);
CREATE INDEX idx_stock_movements_warehouse ON stock_movements (warehouse_id);

-- New company setting: allow stock to go below zero (off by default).
ALTER TABLE company_settings
    ADD COLUMN allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE;

-- Every existing company starts with one warehouse; new companies get theirs at bootstrap.
INSERT INTO warehouses (company_id, name)
SELECT id, 'Almacén principal'
FROM companies;
