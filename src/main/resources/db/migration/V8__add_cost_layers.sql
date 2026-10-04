-- Real inventory cost: every purchase / entry that brings stock into a warehouse is kept as a "cost layer" with the
-- unit cost it really had. Stock that leaves consumes layers, so the value of what remains is the sum of
-- layer quantity x layer cost: exact, not an average.
--
-- stock_levels stays the authority for HOW MANY units there are; the layers say what those units cost. A layer of a
-- product that controls expiry carries the lot it belongs to (expiry_date + lot_number), so choosing a lot also
-- decides which cost leaves. Units without a lot have expiry_date NULL.
CREATE TABLE stock_cost_layers
(
    id           BIGSERIAL PRIMARY KEY,
    company_id   BIGINT         NOT NULL,
    warehouse_id BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,
    unit_cost    NUMERIC(14, 4) NOT NULL,
    -- what is left of this layer; a layer that reaches zero is deleted
    quantity     NUMERIC(14, 3) NOT NULL,
    expiry_date  DATE,
    lot_number   VARCHAR(50)    NOT NULL DEFAULT '',
    received_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cost_layer_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_cost_layer_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_cost_layer_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_cost_layer_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_cost_layer_cost CHECK (unit_cost >= 0)
);

CREATE INDEX idx_cost_layers_lookup
    ON stock_cost_layers (warehouse_id, product_id, expiry_date, lot_number, received_at, id);
CREATE INDEX idx_cost_layers_company ON stock_cost_layers (company_id);

-- Starting point for the stock that already exists: it didn't record what each purchase cost, so it enters at the
-- product's current catalog cost. From here on every unit has its real cost.
-- 1) lots of products that control expiry
INSERT INTO stock_cost_layers (company_id, warehouse_id, product_id, unit_cost, quantity, expiry_date, lot_number)
SELECT l.company_id, l.warehouse_id, l.product_id, p.cost_price, l.quantity, l.expiry_date, l.lot_number
FROM stock_lots l
         JOIN products p ON p.id = l.product_id
WHERE l.quantity > 0;

-- 2) everything else: the stock level minus what the lots already account for
INSERT INTO stock_cost_layers (company_id, warehouse_id, product_id, unit_cost, quantity, expiry_date, lot_number)
SELECT sl.company_id,
       sl.warehouse_id,
       sl.product_id,
       p.cost_price,
       sl.quantity - COALESCE(ls.total, 0),
       NULL,
       ''
FROM stock_levels sl
         JOIN products p ON p.id = sl.product_id
         LEFT JOIN (SELECT warehouse_id, product_id, SUM(quantity) AS total
                    FROM stock_lots
                    GROUP BY warehouse_id, product_id) ls
                   ON ls.warehouse_id = sl.warehouse_id AND ls.product_id = sl.product_id
WHERE sl.quantity - COALESCE(ls.total, 0) > 0;

-- How a company wants the cost of what leaves to be decided: FIFO = the oldest purchase first (nobody has to
-- choose); MANUAL = the user picks which purchase an exit comes from.
ALTER TABLE company_settings
    ADD COLUMN cost_method VARCHAR(10) NOT NULL DEFAULT 'FIFO';
ALTER TABLE company_settings
    ADD CONSTRAINT chk_settings_cost_method CHECK (cost_method IN ('FIFO', 'MANUAL'));
