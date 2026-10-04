package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.CostLayer;
import com.dafacha.inventory.domain.ports.out.CostLayerRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Cost layers are hand-written SQL: locking reads (FOR UPDATE), ordered by age, and a delete-when-empty update. */
@RequiredArgsConstructor
public class CostLayerRepositoryAdapter implements CostLayerRepositoryPort {

    private static final String INSERT_SQL = """
            INSERT INTO stock_cost_layers (company_id, warehouse_id, product_id, unit_cost, quantity, expiry_date, lot_number)
            VALUES (:companyId, :warehouseId, :productId, :unitCost, :quantity, :expiryDate, :lotNumber)
            """;

    private static final String COLUMNS = """
            SELECT id, company_id, warehouse_id, product_id, unit_cost, quantity, expiry_date, lot_number, received_at
            FROM stock_cost_layers
            """;

    /** "IS NOT DISTINCT FROM" so a NULL expiry date (units without a lot) matches only layers without a lot. */
    private static final String LOCK_LOT_SQL = COLUMNS + """
            WHERE warehouse_id = :warehouseId AND product_id = :productId
              AND expiry_date IS NOT DISTINCT FROM CAST(:expiryDate AS DATE)
              AND lot_number = :lotNumber
              AND quantity > 0
            ORDER BY received_at ASC, id ASC
            FOR UPDATE
            """;

    private static final String LOCK_ALL_SQL = COLUMNS + """
            WHERE warehouse_id = :warehouseId AND product_id = :productId AND quantity > 0
            ORDER BY received_at ASC, id ASC
            FOR UPDATE
            """;

    private static final String AVAILABLE_SQL = COLUMNS + """
            WHERE warehouse_id = :warehouseId AND product_id = :productId AND quantity > 0
            ORDER BY received_at ASC, id ASC
            """;

    private static final String CONSUME_SQL = "UPDATE stock_cost_layers SET quantity = quantity - :quantity WHERE id = :id";
    private static final String DELETE_EMPTY_SQL = "DELETE FROM stock_cost_layers WHERE id = :id AND quantity <= 0";

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Void> add(Long companyId, Long warehouseId, Long productId, BigDecimal unitCost, BigDecimal quantity,
                          LocalDate expiryDate, String lotNumber) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(INSERT_SQL)
                .bind("companyId", companyId)
                .bind("warehouseId", warehouseId)
                .bind("productId", productId)
                .bind("unitCost", unitCost)
                .bind("quantity", quantity)
                .bind("lotNumber", lotNumber == null ? "" : lotNumber);
        spec = expiryDate == null ? spec.bindNull("expiryDate", LocalDate.class) : spec.bind("expiryDate", expiryDate);
        return spec.fetch().rowsUpdated().then();
    }

    @Override
    public Flux<CostLayer> lockOldestFirst(Long warehouseId, Long productId, LocalDate expiryDate, String lotNumber) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(LOCK_LOT_SQL)
                .bind("warehouseId", warehouseId)
                .bind("productId", productId)
                .bind("lotNumber", lotNumber == null ? "" : lotNumber);
        spec = expiryDate == null ? spec.bindNull("expiryDate", LocalDate.class) : spec.bind("expiryDate", expiryDate);
        return spec.map((row, metadata) -> read(row)).all();
    }

    @Override
    public Flux<CostLayer> lockAllOldestFirst(Long warehouseId, Long productId) {
        return databaseClient.sql(LOCK_ALL_SQL)
                .bind("warehouseId", warehouseId)
                .bind("productId", productId)
                .map((row, metadata) -> read(row))
                .all();
    }

    @Override
    public Mono<Void> consume(Long layerId, BigDecimal quantity) {
        return databaseClient.sql(CONSUME_SQL)
                .bind("id", layerId)
                .bind("quantity", quantity)
                .fetch()
                .rowsUpdated()
                .then(Mono.defer(() -> databaseClient.sql(DELETE_EMPTY_SQL)
                        .bind("id", layerId)
                        .fetch()
                        .rowsUpdated()))
                .then();
    }

    @Override
    public Flux<CostLayer> findAvailable(Long warehouseId, Long productId) {
        return databaseClient.sql(AVAILABLE_SQL)
                .bind("warehouseId", warehouseId)
                .bind("productId", productId)
                .map((row, metadata) -> read(row))
                .all();
    }

    private CostLayer read(io.r2dbc.spi.Readable row) {
        return CostLayer.builder()
                .id(row.get("id", Long.class))
                .companyId(row.get("company_id", Long.class))
                .warehouseId(row.get("warehouse_id", Long.class))
                .productId(row.get("product_id", Long.class))
                .unitCost(row.get("unit_cost", BigDecimal.class))
                .quantity(row.get("quantity", BigDecimal.class))
                .expiryDate(row.get("expiry_date", LocalDate.class))
                .lotNumber(row.get("lot_number", String.class))
                .receivedAt(row.get("received_at", LocalDateTime.class))
                .build();
    }
}
