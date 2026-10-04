-- Stage 3: suppliers and purchase orders. Receiving an order creates PURCHASE stock movements and
-- updates the product cost by weighted average (see StockRepositoryAdapter.updateWeightedAverageCost).

CREATE TABLE suppliers
(
    id           BIGSERIAL PRIMARY KEY,
    company_id   BIGINT       NOT NULL,
    name         VARCHAR(150) NOT NULL,
    ruc_number   VARCHAR(20),
    contact_name VARCHAR(100),
    phone        VARCHAR(20),
    email        VARCHAR(100),
    address      VARCHAR(200),
    notes        TEXT,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_supplier_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_suppliers_company_name ON suppliers (company_id, LOWER(name));
CREATE UNIQUE INDEX uq_suppliers_company_ruc ON suppliers (company_id, ruc_number) WHERE ruc_number IS NOT NULL;

-- Per-company counters for human-readable document numbers (OC-000001, and later sales / invoices).
-- The counter is bumped with a single atomic upsert, so concurrent requests never get the same number.
CREATE TABLE document_sequences
(
    company_id BIGINT      NOT NULL,
    name       VARCHAR(40) NOT NULL,
    last_value BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (company_id, name),
    CONSTRAINT fk_document_sequence_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE
);

CREATE TABLE purchase_orders
(
    id            BIGSERIAL PRIMARY KEY,
    company_id    BIGINT         NOT NULL,
    supplier_id   BIGINT         NOT NULL,
    warehouse_id  BIGINT         NOT NULL, -- where the goods will be received
    number        VARCHAR(20)    NOT NULL,
    status        VARCHAR(25)    NOT NULL DEFAULT 'DRAFT',
    expected_date DATE,
    notes         TEXT,
    total         NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_by    BIGINT,
    ordered_at    TIMESTAMP,
    cancelled_at  TIMESTAMP,
    created_at    TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchase_order_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_order_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_purchase_order_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_purchase_order_user
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uq_purchase_orders_company_number UNIQUE (company_id, number)
);

CREATE INDEX idx_purchase_orders_company_created ON purchase_orders (company_id, created_at DESC);
CREATE INDEX idx_purchase_orders_supplier ON purchase_orders (supplier_id);

CREATE TABLE purchase_order_lines
(
    id                BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT         NOT NULL,
    product_id        BIGINT         NOT NULL,
    quantity          NUMERIC(14, 3) NOT NULL,
    unit_cost         NUMERIC(12, 4) NOT NULL,
    received_quantity NUMERIC(14, 3) NOT NULL DEFAULT 0,
    CONSTRAINT fk_po_line_order
        FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_po_line_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_po_line_quantity CHECK (quantity > 0),
    CONSTRAINT chk_po_line_unit_cost CHECK (unit_cost >= 0),
    CONSTRAINT chk_po_line_received CHECK (received_quantity >= 0 AND received_quantity <= quantity),
    CONSTRAINT uq_po_line_product UNIQUE (purchase_order_id, product_id)
);

CREATE INDEX idx_po_lines_product ON purchase_order_lines (product_id);

-- Stock movements created by receiving an order point back to it.
ALTER TABLE stock_movements
    ADD COLUMN purchase_order_id BIGINT,
    ADD CONSTRAINT fk_stock_movement_purchase_order
        FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id);

CREATE INDEX idx_stock_movements_purchase_order ON stock_movements (purchase_order_id);
