package com.dafacha.inventory.infrastructure.adapter.out.repository;

import org.springframework.r2dbc.core.DatabaseClient;

import java.util.List;

/**
 * Limits a query to the warehouses a user may use. {@code allowed == null} means no limit and adds nothing, so the
 * unrestricted queries stay exactly as they were. The list is bound as a collection, which DatabaseClient expands
 * into {@code IN ($1, $2, ...)}; a restricted user always has at least one warehouse, so the list is never empty.
 */
final class WarehouseScopeSql {

    private static final String PARAM = "allowedWarehouses";

    private WarehouseScopeSql() {
    }

    /** {@code " AND <column> IN (:allowedWarehouses)"}, or nothing when there is no restriction. */
    static String and(List<Long> allowed, String column) {
        return allowed == null ? "" : "\n  AND " + column + " IN (:" + PARAM + ")\n";
    }

    static DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec, List<Long> allowed) {
        return allowed == null ? spec : spec.bind(PARAM, allowed);
    }
}
