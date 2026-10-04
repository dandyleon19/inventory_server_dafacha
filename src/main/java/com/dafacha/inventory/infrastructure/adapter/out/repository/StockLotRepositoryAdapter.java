package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.LotScope;
import com.dafacha.inventory.domain.model.StockLot;
import com.dafacha.inventory.domain.model.UnitOfMeasure;
import com.dafacha.inventory.domain.ports.out.StockLotRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lots are hand-written SQL on DatabaseClient: locking reads, an upsert on a composite key, and a joined list. */
@RequiredArgsConstructor
public class StockLotRepositoryAdapter implements StockLotRepositoryPort {

    private static final String ADD_SQL = """
            INSERT INTO stock_lots (company_id, warehouse_id, product_id, lot_number, expiry_date, quantity)
            VALUES (:companyId, :warehouseId, :productId, :lotNumber, :expiryDate, :quantity)
            ON CONFLICT (warehouse_id, product_id, expiry_date, lot_number) DO UPDATE
                SET quantity = stock_lots.quantity + EXCLUDED.quantity
            """;

    /** FOR UPDATE: the rows stay locked until the operation's transaction ends, so two exits can't take the same units. */
    private static final String LOCK_FEFO_SQL = """
            SELECT id, lot_number, expiry_date, quantity
            FROM stock_lots
            WHERE warehouse_id = :warehouseId AND product_id = :productId AND quantity > 0
            ORDER BY expiry_date ASC, id ASC
            FOR UPDATE
            """;

    private static final String EXISTS_WITH_STOCK_SQL =
            "SELECT EXISTS (SELECT 1 FROM stock_lots WHERE product_id = :productId AND quantity > 0) AS present";

    private static final String CONSUME_SQL = "UPDATE stock_lots SET quantity = quantity - :quantity WHERE id = :id";
    private static final String DELETE_EMPTY_SQL = "DELETE FROM stock_lots WHERE id = :id AND quantity <= 0";

    /**
     * Which lots a scope means, relative to the database's CURRENT_DATE:
     * EXPIRED = before today; EXPIRING = from today up to :days from now; ATTENTION = both.
     */
    private static String filters(List<Long> allowed) {
        return """
                FROM stock_lots l
                JOIN warehouses w ON w.id = l.warehouse_id
                JOIN products p ON p.id = l.product_id
                WHERE l.company_id = :companyId
                  AND l.quantity > 0
                  AND (CAST(:productId AS BIGINT) IS NULL OR l.product_id = :productId)
                  AND (CAST(:warehouseId AS BIGINT) IS NULL OR l.warehouse_id = :warehouseId)
                  AND (
                      :scope = 'ALL'
                      OR (:scope = 'EXPIRED' AND l.expiry_date < CURRENT_DATE)
                      OR (:scope = 'EXPIRING' AND l.expiry_date >= CURRENT_DATE
                                              AND l.expiry_date <= CURRENT_DATE + CAST(:days AS INTEGER))
                      OR (:scope = 'ATTENTION' AND l.expiry_date <= CURRENT_DATE + CAST(:days AS INTEGER))
                  )
                """ + WarehouseScopeSql.and(allowed, "l.warehouse_id");
    }

    private static String pageSql(List<Long> allowed) {
        return """
                SELECT l.id, l.company_id, l.warehouse_id, w.name AS warehouse_name,
                       l.product_id, p.name AS product_name, p.sku AS product_sku, p.unit,
                       l.lot_number, l.expiry_date, l.quantity,
                       (l.expiry_date - CURRENT_DATE) AS days_to_expiry
                """ + filters(allowed) + """
                ORDER BY l.expiry_date ASC, l.id ASC
                LIMIT :limit OFFSET :offset
                """;
    }

    private static String countSql(List<Long> allowed) {
        return "SELECT COUNT(*) AS total " + filters(allowed);
    }

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Void> addToLot(Long companyId, Long warehouseId, Long productId, String lotNumber,
                               LocalDate expiryDate, BigDecimal quantity) {
        return databaseClient.sql(ADD_SQL)
                .bind("companyId", companyId)
                .bind("warehouseId", warehouseId)
                .bind("productId", productId)
                .bind("lotNumber", lotNumber == null ? "" : lotNumber)
                .bind("expiryDate", expiryDate)
                .bind("quantity", quantity)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Flux<StockLot> lockFefo(Long warehouseId, Long productId) {
        return databaseClient.sql(LOCK_FEFO_SQL)
                .bind("warehouseId", warehouseId)
                .bind("productId", productId)
                .map(row -> StockLot.builder()
                        .id(row.get("id", Long.class))
                        .lotNumber(row.get("lot_number", String.class))
                        .expiryDate(row.get("expiry_date", LocalDate.class))
                        .quantity(row.get("quantity", BigDecimal.class))
                        .build())
                .all();
    }

    @Override
    public Mono<Boolean> existsWithStock(Long productId) {
        return databaseClient.sql(EXISTS_WITH_STOCK_SQL)
                .bind("productId", productId)
                .map(row -> row.get("present", Boolean.class))
                .one();
    }

    @Override
    public Mono<Void> consume(Long lotId, BigDecimal quantity) {
        return databaseClient.sql(CONSUME_SQL)
                .bind("id", lotId)
                .bind("quantity", quantity)
                .fetch()
                .rowsUpdated()
                .then(Mono.defer(() -> databaseClient.sql(DELETE_EMPTY_SQL)
                        .bind("id", lotId)
                        .fetch()
                        .rowsUpdated()))
                .then();
    }

    @Override
    public Flux<StockLot> findPage(Long companyId, Long productId, Long warehouseId, List<Long> allowedWarehouseIds,
                                   LotScope scope, int days, int page, int size) {
        return bindFilters(databaseClient.sql(pageSql(allowedWarehouseIds)), companyId, productId, warehouseId,
                allowedWarehouseIds, scope, days)
                .bind("limit", size)
                .bind("offset", (long) page * size)
                .map(row -> StockLot.builder()
                        .id(row.get("id", Long.class))
                        .companyId(row.get("company_id", Long.class))
                        .warehouseId(row.get("warehouse_id", Long.class))
                        .warehouseName(row.get("warehouse_name", String.class))
                        .productId(row.get("product_id", Long.class))
                        .productName(row.get("product_name", String.class))
                        .productSku(row.get("product_sku", String.class))
                        .unit(UnitOfMeasure.valueOf(row.get("unit", String.class)))
                        .lotNumber(row.get("lot_number", String.class))
                        .expiryDate(row.get("expiry_date", LocalDate.class))
                        .quantity(row.get("quantity", BigDecimal.class))
                        .daysToExpiry(row.get("days_to_expiry", Integer.class))
                        .build())
                .all();
    }

    @Override
    public Mono<Long> count(Long companyId, Long productId, Long warehouseId, List<Long> allowedWarehouseIds,
                            LotScope scope, int days) {
        return bindFilters(databaseClient.sql(countSql(allowedWarehouseIds)), companyId, productId, warehouseId,
                allowedWarehouseIds, scope, days)
                .map(row -> row.get("total", Long.class))
                .one();
    }

    /** Null filters must be bound with an explicit type, otherwise PostgreSQL can't infer the parameter type. */
    private DatabaseClient.GenericExecuteSpec bindFilters(DatabaseClient.GenericExecuteSpec spec, Long companyId,
                                                          Long productId, Long warehouseId, List<Long> allowed,
                                                          LotScope scope, int days) {
        spec = WarehouseScopeSql.bind(spec.bind("companyId", companyId)
                .bind("scope", scope.name())
                .bind("days", days), allowed);
        spec = productId == null ? spec.bindNull("productId", Long.class) : spec.bind("productId", productId);
        spec = warehouseId == null ? spec.bindNull("warehouseId", Long.class) : spec.bind("warehouseId", warehouseId);
        return spec;
    }
}
