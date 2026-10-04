-- Expiry control: products can be flagged as "tracks expiry"; their stock is then kept in lots
-- (warehouse + product + expiry date + optional lot number) and consumed first-expired-first-out.

ALTER TABLE products
    ADD COLUMN tracks_expiry BOOLEAN NOT NULL DEFAULT FALSE;

-- Remaining quantity per lot. stock_levels stays the total per (warehouse, product); for a tracked product the
-- lots add up to it, except for stock that existed before the product started tracking expiry ("no lot"), which
-- has no row here and is consumed last. Empty lots are deleted, so every row has quantity > 0 at rest.
-- lot_number is '' (not NULL) when there is none, so the unique key below needs no expression.
CREATE TABLE stock_lots
(
    id           BIGSERIAL PRIMARY KEY,
    company_id   BIGINT         NOT NULL,
    warehouse_id BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,
    lot_number   VARCHAR(50)    NOT NULL DEFAULT '',
    expiry_date  DATE           NOT NULL,
    quantity     NUMERIC(14, 3) NOT NULL,
    created_at   TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_lot_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_lot_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_lot_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_stock_lot_quantity CHECK (quantity >= 0),
    CONSTRAINT uq_stock_lot UNIQUE (warehouse_id, product_id, expiry_date, lot_number)
);

CREATE INDEX idx_stock_lots_company_expiry ON stock_lots (company_id, expiry_date);
CREATE INDEX idx_stock_lots_product ON stock_lots (product_id);

-- Movements that touch a lot say which one (a FEFO exit spanning two lots is logged as two rows).
ALTER TABLE stock_movements
    ADD COLUMN lot_number  VARCHAR(50),
    ADD COLUMN expiry_date DATE;

-- Alert thresholds, per company.
ALTER TABLE company_settings
    ADD COLUMN expiry_alert_days           INTEGER NOT NULL DEFAULT 30,
    ADD COLUMN purchase_arrival_alert_days INTEGER NOT NULL DEFAULT 7,
    ADD CONSTRAINT chk_settings_expiry_alert_days CHECK (expiry_alert_days BETWEEN 1 AND 365),
    ADD CONSTRAINT chk_settings_arrival_alert_days CHECK (purchase_arrival_alert_days BETWEEN 1 AND 90);
