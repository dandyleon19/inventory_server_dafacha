-- Stage 8: more roles, per-user warehouse access and an audit log.

-- The old STAFF_USER had exactly the permissions the new warehouse role has (entries, exits, transfers, receiving).
UPDATE users SET role = 'WAREHOUSE_USER' WHERE role = 'STAFF_USER';

-- A user with NO rows here can use every warehouse of the company; with rows, only those.
-- (Admins are never restricted, whatever is stored here.)
CREATE TABLE user_warehouses
(
    user_id      BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, warehouse_id),
    CONSTRAINT fk_user_warehouse_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_warehouse_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_warehouses_warehouse ON user_warehouses (warehouse_id);

-- Who did what. The user's name is copied into the row, so the trail still reads right if the user is renamed.
-- No foreign keys on purpose: the log must outlive whatever it refers to (a deleted product, say).
CREATE TABLE audit_log
(
    id           BIGSERIAL PRIMARY KEY,
    company_id   BIGINT       NOT NULL,
    user_id      BIGINT,
    user_name    VARCHAR(255),
    action       VARCHAR(30)  NOT NULL,
    entity_type  VARCHAR(40)  NOT NULL,
    entity_id    BIGINT,
    entity_label VARCHAR(255),
    details      TEXT,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_company_created ON audit_log (company_id, created_at DESC);
CREATE INDEX idx_audit_log_company_entity ON audit_log (company_id, entity_type, entity_id);
