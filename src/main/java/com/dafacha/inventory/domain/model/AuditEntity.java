package com.dafacha.inventory.domain.model;

/** What an audit entry is about. */
public enum AuditEntity {
    PRODUCT,
    CATEGORY,
    SUPPLIER,
    WAREHOUSE,
    PURCHASE_ORDER,
    USER,
    COMPANY,
    SETTINGS,
    SESSION
}
