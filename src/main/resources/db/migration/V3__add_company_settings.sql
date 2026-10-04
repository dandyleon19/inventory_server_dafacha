-- Per-company settings. Rows are optional: a company without a row uses the defaults declared here
-- (mirrored in CompanySettings.defaults()), and a row is created the first time it saves settings.
CREATE TABLE company_settings
(
    company_id               BIGINT PRIMARY KEY,
    require_product_sku      BOOLEAN NOT NULL DEFAULT FALSE,
    require_product_category BOOLEAN NOT NULL DEFAULT FALSE,
    require_product_barcode  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_company_settings_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE
);

-- SKU becomes optional at the schema level; whether it is mandatory is now a company setting.
-- The unique index on (company_id, LOWER(sku)) already ignores NULLs, so many products may have no SKU.
ALTER TABLE products
    ALTER COLUMN sku DROP NOT NULL;
