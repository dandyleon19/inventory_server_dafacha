package com.dafacha.inventory.domain.model;

/**
 * Fixed roles, each with a predefined set of permissions (the matrix lives in SecurityConfig):
 * <ul>
 *   <li>SUPER_ADMIN / ADMIN_USER: everything, including users, settings, catalog and adjustments.</li>
 *   <li>WAREHOUSE_USER: day-to-day stock work (entries, exits, transfers, receiving goods).</li>
 *   <li>PURCHASING_USER: suppliers and purchase orders (and receiving them).</li>
 *   <li>VIEWER: read-only.</li>
 * </ul>
 */
public enum Role {
    SUPER_ADMIN,
    ADMIN_USER,
    WAREHOUSE_USER,
    PURCHASING_USER,
    VIEWER;

    /** Admins manage everything and are never limited to a subset of warehouses. */
    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == ADMIN_USER;
    }
}
