package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.MovementType;
import com.dafacha.inventory.domain.model.StockMovement;
import com.dafacha.inventory.domain.ports.out.StockMovementRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.StockMovementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Saving and existence checks go through the Spring Data repository; the history list is a hand-written
 * query with joins (warehouse, product and user names), so it uses DatabaseClient.
 */
@RequiredArgsConstructor
public class StockMovementRepositoryAdapter implements StockMovementRepositoryPort {

    private static String filters(List<Long> allowed) {
        return """
                WHERE m.company_id = :companyId
                  AND (CAST(:productId AS BIGINT) IS NULL OR m.product_id = :productId)
                  AND (CAST(:warehouseId AS BIGINT) IS NULL OR m.warehouse_id = :warehouseId)
                  AND (CAST(:type AS TEXT) IS NULL OR m.type = :type)
                  AND (CAST(:purchaseOrderId AS BIGINT) IS NULL OR m.purchase_order_id = :purchaseOrderId)
                """ + WarehouseScopeSql.and(allowed, "m.warehouse_id");
    }

    private static final String PAGE_HEAD = """
            SELECT m.id, m.company_id, m.warehouse_id, w.name AS warehouse_name,
                   m.product_id, p.name AS product_name, p.sku AS product_sku,
                   m.type, m.quantity, m.balance_after, m.unit_cost, m.notes, m.transfer_ref,
                   m.purchase_order_id, po.number AS purchase_order_number, sup.name AS supplier_name,
                   m.lot_number, m.expiry_date,
                   m.user_id, TRIM(CONCAT(u.first_name, ' ', u.last_name)) AS user_name, m.created_at
            FROM stock_movements m
            JOIN warehouses w ON w.id = m.warehouse_id
            JOIN products p ON p.id = m.product_id
            LEFT JOIN users u ON u.id = m.user_id
            LEFT JOIN purchase_orders po ON po.id = m.purchase_order_id
            LEFT JOIN suppliers sup ON sup.id = po.supplier_id
            """;

    private static String pageSql(List<Long> allowed) {
        return PAGE_HEAD + filters(allowed) + """
                ORDER BY m.created_at DESC, m.id DESC
                LIMIT :limit OFFSET :offset
                """;
    }

    private static String countSql(List<Long> allowed) {
        return "SELECT COUNT(*) AS total FROM stock_movements m " + filters(allowed);
    }

    private final StockMovementRepository repository;
    private final StockMovementMapper mapper;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<StockMovement> save(StockMovement movement) {
        return repository.save(mapper.toEntity(movement)).map(mapper::toDomain);
    }

    @Override
    public Flux<StockMovement> findPage(Long companyId, Long productId, Long warehouseId,
                                        List<Long> allowedWarehouseIds, MovementType type, Long purchaseOrderId,
                                        int page, int size) {
        return bindFilters(databaseClient.sql(pageSql(allowedWarehouseIds)), companyId, productId, warehouseId,
                allowedWarehouseIds, type, purchaseOrderId)
                .bind("limit", size)
                .bind("offset", (long) page * size)
                .map(row -> StockMovement.builder()
                        .id(row.get("id", Long.class))
                        .companyId(row.get("company_id", Long.class))
                        .warehouseId(row.get("warehouse_id", Long.class))
                        .warehouseName(row.get("warehouse_name", String.class))
                        .productId(row.get("product_id", Long.class))
                        .productName(row.get("product_name", String.class))
                        .productSku(row.get("product_sku", String.class))
                        .type(MovementType.valueOf(row.get("type", String.class)))
                        .quantity(row.get("quantity", BigDecimal.class))
                        .balanceAfter(row.get("balance_after", BigDecimal.class))
                        .unitCost(row.get("unit_cost", BigDecimal.class))
                        .notes(row.get("notes", String.class))
                        .transferRef(row.get("transfer_ref", String.class))
                        .purchaseOrderId(row.get("purchase_order_id", Long.class))
                        .purchaseOrderNumber(row.get("purchase_order_number", String.class))
                        .supplierName(row.get("supplier_name", String.class))
                        .lotNumber(row.get("lot_number", String.class))
                        .expiryDate(row.get("expiry_date", LocalDate.class))
                        .userId(row.get("user_id", Long.class))
                        .userName(row.get("user_name", String.class))
                        .createdAt(row.get("created_at", LocalDateTime.class))
                        .build())
                .all();
    }

    @Override
    public Mono<Long> count(Long companyId, Long productId, Long warehouseId, List<Long> allowedWarehouseIds,
                            MovementType type, Long purchaseOrderId) {
        return bindFilters(databaseClient.sql(countSql(allowedWarehouseIds)), companyId, productId, warehouseId,
                allowedWarehouseIds, type, purchaseOrderId)
                .map(row -> row.get("total", Long.class))
                .one();
    }

    @Override
    public Mono<Boolean> existsByWarehouseId(Long warehouseId) {
        return repository.existsByWarehouse(warehouseId);
    }

    @Override
    public Mono<Boolean> existsByProductId(Long productId) {
        return repository.existsByProduct(productId);
    }

    /** Null filters must be bound with an explicit type, otherwise PostgreSQL can't infer the parameter type. */
    private DatabaseClient.GenericExecuteSpec bindFilters(DatabaseClient.GenericExecuteSpec spec, Long companyId,
                                                          Long productId, Long warehouseId, List<Long> allowed,
                                                          MovementType type, Long purchaseOrderId) {
        spec = WarehouseScopeSql.bind(spec.bind("companyId", companyId), allowed);
        spec = purchaseOrderId == null
                ? spec.bindNull("purchaseOrderId", Long.class)
                : spec.bind("purchaseOrderId", purchaseOrderId);
        spec = productId == null ? spec.bindNull("productId", Long.class) : spec.bind("productId", productId);
        spec = warehouseId == null ? spec.bindNull("warehouseId", Long.class) : spec.bind("warehouseId", warehouseId);
        spec = type == null ? spec.bindNull("type", String.class) : spec.bind("type", type.name());
        return spec;
    }
}
