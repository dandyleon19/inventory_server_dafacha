package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.StockSummary;
import com.dafacha.inventory.domain.model.UnitOfMeasure;
import com.dafacha.inventory.domain.model.WarehouseValuation;
import com.dafacha.inventory.domain.ports.out.StockRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Stock queries are hand-written SQL (atomic upsert with RETURNING, aggregates, joins), so this adapter uses
 * DatabaseClient directly instead of a Spring Data repository: stock_levels has a composite key and there
 * is no entity worth mapping.
 */
@RequiredArgsConstructor
public class StockRepositoryAdapter implements StockRepositoryPort {

    /**
     * Step 1 of applying a change: make sure the (warehouse, product) row exists, starting at zero.
     * Idempotent and harmless on its own; the rows it creates are rolled back with the operation if it fails.
     */
    private static final String ENSURE_ROW_SQL = """
            INSERT INTO stock_levels (company_id, warehouse_id, product_id, quantity)
            VALUES (:companyId, :warehouseId, :productId, 0)
            ON CONFLICT (warehouse_id, product_id) DO NOTHING
            """;

    /**
     * Step 2, the "check and change" in one atomic statement: the row is updated only if the result is allowed
     * (not negative, unless the company permits it), and RETURNING yields nothing when it isn't. The row lock
     * taken by UPDATE serializes concurrent operations on the same stock, so there is no read-then-write race.
     */
    private static final String APPLY_DELTA_SQL = """
            UPDATE stock_levels
            SET quantity = quantity + CAST(:delta AS NUMERIC),
                updated_at = CURRENT_TIMESTAMP
            WHERE warehouse_id = :warehouseId
              AND product_id = :productId
              AND (quantity + CAST(:delta AS NUMERIC) >= 0 OR :allowNegative)
            RETURNING quantity
            """;

    /**
     * Weighted average in a single statement: (stock x current cost + incoming x incoming cost) / (stock + incoming).
     * The stock total is read in the same statement, so it must run before the incoming quantity is added.
     */
    private static final String WEIGHTED_COST_SQL = """
            UPDATE products p
            SET cost_price = CASE
                    WHEN s.total <= 0 THEN CAST(:unitCost AS NUMERIC)
                    ELSE ROUND((s.total * p.cost_price + CAST(:quantity AS NUMERIC) * CAST(:unitCost AS NUMERIC))
                               / (s.total + CAST(:quantity AS NUMERIC)), 4)
                END,
                updated_at = CURRENT_TIMESTAMP
            FROM (SELECT COALESCE(SUM(quantity), 0) AS total FROM stock_levels WHERE product_id = :productId) s
            WHERE p.id = :productId AND p.company_id = :companyId
            RETURNING p.cost_price
            """;

    private static final String EXISTS_STOCK_SQL =
            "SELECT EXISTS (SELECT 1 FROM stock_levels WHERE product_id = :productId AND quantity <> 0) AS present";

    private static final String FIND_QUANTITY_SQL =
            "SELECT quantity FROM stock_levels WHERE warehouse_id = :warehouseId AND product_id = :productId";

    private static String summaryBody(List<Long> allowed) {
        return """
                FROM products p
                LEFT JOIN stock_levels sl
                       ON sl.product_id = p.id
                      AND (CAST(:warehouseId AS BIGINT) IS NULL OR sl.warehouse_id = :warehouseId)
                """ + WarehouseScopeSql.and(allowed, "sl.warehouse_id") + """
                WHERE p.company_id = :companyId
                  AND (CAST(:productId AS BIGINT) IS NULL OR p.id = :productId)
                  AND (CAST(:isActive AS BOOLEAN) IS NULL OR p.is_active = :isActive)
                  AND (
                      CAST(:search AS TEXT) IS NULL
                      OR p.name ILIKE :search
                      OR p.sku ILIKE :search
                      OR p.barcode ILIKE :search
                  )
                GROUP BY p.id
                HAVING (NOT :lowStockOnly
                        OR (p.min_stock > 0 AND COALESCE(SUM(sl.quantity), 0) <= p.min_stock))
                """;
    }

    private static String summaryPageSql(List<Long> allowed) {
        return """
                SELECT p.id AS product_id, p.sku, p.name, p.unit, p.min_stock, p.cost_price, p.tracks_expiry, p.is_active,
                       COALESCE(SUM(sl.quantity), 0) AS quantity,
                       (SELECT COALESCE(SUM(cl.quantity * cl.unit_cost), 0)
                        FROM stock_cost_layers cl
                        WHERE cl.product_id = p.id AND cl.quantity > 0
                          AND (CAST(:warehouseId AS BIGINT) IS NULL OR cl.warehouse_id = :warehouseId)
                """ + WarehouseScopeSql.and(allowed, "cl.warehouse_id") + """
                       ) AS layer_value
                """ + summaryBody(allowed) + """
                ORDER BY p.name ASC, p.id ASC
                LIMIT :limit OFFSET :offset
                """;
    }

    private static String summaryCountSql(List<Long> allowed) {
        return "SELECT COUNT(*) AS total FROM (SELECT p.id " + summaryBody(allowed) + ") counted";
    }

    /**
     * The REAL value: every unit in the warehouse is in a cost layer with the cost it had, so the value is the sum of
     * quantity x layer cost (not stock x one average cost).
     */
    private static String valuationSql(List<Long> allowed) {
        return """
                SELECT w.id AS warehouse_id, w.name AS warehouse_name,
                       COALESCE(SUM(cl.quantity * cl.unit_cost), 0) AS value,
                       COUNT(DISTINCT cl.product_id) AS product_count
                FROM warehouses w
                LEFT JOIN stock_cost_layers cl ON cl.warehouse_id = w.id AND cl.quantity > 0
                WHERE w.company_id = :companyId
                """ + WarehouseScopeSql.and(allowed, "w.id") + """
                GROUP BY w.id, w.name
                ORDER BY w.name ASC, w.id ASC
                """;
    }

    private final DatabaseClient databaseClient;

    @Override
    public Mono<BigDecimal> applyDelta(Long companyId, Long warehouseId, Long productId, BigDecimal delta,
                                       boolean allowNegative) {
        return databaseClient.sql(ENSURE_ROW_SQL)
                .bind("companyId", companyId)
                .bind("warehouseId", warehouseId)
                .bind("productId", productId)
                .fetch()
                .rowsUpdated()
                .then(Mono.defer(() -> databaseClient.sql(APPLY_DELTA_SQL)
                        .bind("warehouseId", warehouseId)
                        .bind("productId", productId)
                        .bind("delta", delta)
                        .bind("allowNegative", allowNegative)
                        .map(row -> row.get("quantity", BigDecimal.class))
                        .one()));
    }

    @Override
    public Mono<BigDecimal> updateWeightedAverageCost(Long companyId, Long productId, BigDecimal quantity,
                                                      BigDecimal unitCost) {
        return databaseClient.sql(WEIGHTED_COST_SQL)
                .bind("companyId", companyId)
                .bind("productId", productId)
                .bind("quantity", quantity)
                .bind("unitCost", unitCost)
                .map(row -> row.get("cost_price", BigDecimal.class))
                .one();
    }

    @Override
    public Mono<Boolean> existsStock(Long productId) {
        return databaseClient.sql(EXISTS_STOCK_SQL)
                .bind("productId", productId)
                .map(row -> row.get("present", Boolean.class))
                .one();
    }

    @Override
    public Mono<BigDecimal> findQuantity(Long warehouseId, Long productId) {
        return databaseClient.sql(FIND_QUANTITY_SQL)
                .bind("warehouseId", warehouseId)
                .bind("productId", productId)
                .map(row -> row.get("quantity", BigDecimal.class))
                .one();
    }

    @Override
    public Flux<StockSummary> findSummaryPage(Long companyId, Long productId, Long warehouseId,
                                              List<Long> allowedWarehouseIds, Boolean isActive, String searchPattern,
                                              boolean lowStockOnly, int page, int size) {
        return bindSummaryFilters(databaseClient.sql(summaryPageSql(allowedWarehouseIds)), companyId, productId,
                        warehouseId, allowedWarehouseIds, isActive, searchPattern, lowStockOnly)
                .bind("limit", size)
                .bind("offset", (long) page * size)
                .map(row -> StockSummary.builder()
                        .productId(row.get("product_id", Long.class))
                        .sku(row.get("sku", String.class))
                        .productName(row.get("name", String.class))
                        .unit(UnitOfMeasure.valueOf(row.get("unit", String.class)))
                        .minStock(row.get("min_stock", BigDecimal.class))
                        .costPrice(row.get("cost_price", BigDecimal.class))
                        .tracksExpiry(row.get("tracks_expiry", Boolean.class))
                        .isActive(row.get("is_active", Boolean.class))
                        .quantity(row.get("quantity", BigDecimal.class))
                        .realValue(row.get("layer_value", BigDecimal.class))
                        .build())
                .all();
    }

    @Override
    public Mono<Long> countSummary(Long companyId, Long productId, Long warehouseId, List<Long> allowedWarehouseIds,
                                   Boolean isActive, String searchPattern, boolean lowStockOnly) {
        return bindSummaryFilters(databaseClient.sql(summaryCountSql(allowedWarehouseIds)), companyId, productId,
                        warehouseId, allowedWarehouseIds, isActive, searchPattern, lowStockOnly)
                .map(row -> row.get("total", Long.class))
                .one();
    }

    @Override
    public Flux<WarehouseValuation> findValuation(Long companyId, List<Long> allowedWarehouseIds) {
        return WarehouseScopeSql.bind(databaseClient.sql(valuationSql(allowedWarehouseIds))
                        .bind("companyId", companyId), allowedWarehouseIds)
                .map(row -> WarehouseValuation.builder()
                        .warehouseId(row.get("warehouse_id", Long.class))
                        .warehouseName(row.get("warehouse_name", String.class))
                        .value(row.get("value", BigDecimal.class))
                        .productCount(row.get("product_count", Long.class))
                        .build())
                .all();
    }

    /** Null filters must be bound with an explicit type, otherwise PostgreSQL can't infer the parameter type. */
    private DatabaseClient.GenericExecuteSpec bindSummaryFilters(DatabaseClient.GenericExecuteSpec spec, Long companyId,
                                                                 Long productId, Long warehouseId, List<Long> allowed,
                                                                 Boolean isActive, String search, boolean lowStockOnly) {
        spec = WarehouseScopeSql.bind(spec.bind("companyId", companyId).bind("lowStockOnly", lowStockOnly), allowed);
        spec = productId == null ? spec.bindNull("productId", Long.class) : spec.bind("productId", productId);
        spec = warehouseId == null ? spec.bindNull("warehouseId", Long.class) : spec.bind("warehouseId", warehouseId);
        spec = isActive == null ? spec.bindNull("isActive", Boolean.class) : spec.bind("isActive", isActive);
        spec = search == null ? spec.bindNull("search", String.class) : spec.bind("search", search);
        return spec;
    }
}
