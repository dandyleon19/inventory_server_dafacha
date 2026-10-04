package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.PurchaseOrder;
import com.dafacha.inventory.domain.model.PurchaseOrderLine;
import com.dafacha.inventory.domain.model.PurchaseOrderStatus;
import com.dafacha.inventory.domain.model.UnitOfMeasure;
import com.dafacha.inventory.domain.ports.out.PurchaseOrderRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.PurchaseOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import io.r2dbc.spi.Readable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Writes go through the Spring Data repositories; reads with joins (supplier / warehouse / user / product
 * names) are hand-written SQL on DatabaseClient.
 */
@RequiredArgsConstructor
public class PurchaseOrderRepositoryAdapter implements PurchaseOrderRepositoryPort {

    private static final String HEADER_COLUMNS = """
            SELECT po.id, po.company_id, po.supplier_id, s.name AS supplier_name,
                   po.warehouse_id, w.name AS warehouse_name, po.number, po.status, po.expected_date,
                   po.notes, po.total, po.created_by, TRIM(CONCAT(u.first_name, ' ', u.last_name)) AS created_by_name,
                   po.ordered_at, po.cancelled_at, po.created_at, po.updated_at
            """;

    private static final String HEADER_FROM = """
            FROM purchase_orders po
            JOIN suppliers s ON s.id = po.supplier_id
            JOIN warehouses w ON w.id = po.warehouse_id
            LEFT JOIN users u ON u.id = po.created_by
            """;

    private static final String DETAIL_SQL = HEADER_COLUMNS + HEADER_FROM + """
            WHERE po.id = :id AND po.company_id = :companyId
            """;

    private static final String LINES_SQL = """
            SELECT l.id, l.purchase_order_id, l.product_id, p.name AS product_name, p.sku AS product_sku, p.unit,
                   p.tracks_expiry, l.quantity, l.unit_cost, l.received_quantity, l.packaging, l.entry_input
            FROM purchase_order_lines l
            JOIN products p ON p.id = l.product_id
            WHERE l.purchase_order_id = :id
            ORDER BY l.id
            """;

    private static String filters(List<Long> allowed) {
        return """
                WHERE po.company_id = :companyId
                  AND (CAST(:status AS TEXT) IS NULL OR po.status = :status)
                  AND (CAST(:supplierId AS BIGINT) IS NULL OR po.supplier_id = :supplierId)
                  AND (CAST(:search AS TEXT) IS NULL OR po.number ILIKE :search OR s.name ILIKE :search)
                """ + WarehouseScopeSql.and(allowed, "po.warehouse_id");
    }

    private static String pageSql(List<Long> allowed) {
        return HEADER_COLUMNS + """
                       , (SELECT COUNT(*) FROM purchase_order_lines l WHERE l.purchase_order_id = po.id) AS line_count
                """ + HEADER_FROM + filters(allowed) + """
                ORDER BY po.created_at DESC, po.id DESC
                LIMIT :limit OFFSET :offset
                """;
    }

    private static String countSql(List<Long> allowed) {
        return "SELECT COUNT(*) AS total_count " + HEADER_FROM + filters(allowed);
    }

    /** Orders still open (nothing finished or cancelled) that have an expected date within the window, overdue included. */
    private static String arrivingFilter(List<Long> allowed) {
        return """
                WHERE po.company_id = :companyId
                  AND po.status IN ('ORDERED', 'PARTIALLY_RECEIVED')
                  AND po.expected_date IS NOT NULL
                  AND po.expected_date <= CURRENT_DATE + CAST(:days AS INTEGER)
                """ + WarehouseScopeSql.and(allowed, "po.warehouse_id");
    }

    private static String arrivingSql(List<Long> allowed) {
        return HEADER_COLUMNS + HEADER_FROM + arrivingFilter(allowed) + """
                ORDER BY po.expected_date ASC, po.id ASC
                LIMIT :limit
                """;
    }

    private static String arrivingCountSql(List<Long> allowed) {
        return "SELECT COUNT(*) AS total_count " + HEADER_FROM + arrivingFilter(allowed);
    }

    private static String overdueCountSql(List<Long> allowed) {
        return """
                SELECT COUNT(*) AS total_count
                FROM purchase_orders po
                WHERE po.company_id = :companyId
                  AND po.status IN ('ORDERED', 'PARTIALLY_RECEIVED')
                  AND po.expected_date IS NOT NULL
                  AND po.expected_date < CURRENT_DATE
                """ + WarehouseScopeSql.and(allowed, "po.warehouse_id");
    }

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderLineRepository lineRepository;
    private final PurchaseOrderMapper mapper;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<PurchaseOrder> saveHeader(PurchaseOrder order) {
        return orderRepository.save(mapper.toEntity(order)).map(mapper::toDomain);
    }

    @Override
    public Mono<Void> replaceLines(Long purchaseOrderId, List<PurchaseOrderLine> lines) {
        return lineRepository.deleteByOrder(purchaseOrderId)
                .thenMany(lineRepository.saveAll(Flux.fromIterable(lines).map(line -> {
                    line.setId(null);
                    line.setPurchaseOrderId(purchaseOrderId);
                    line.setReceivedQuantity(BigDecimal.ZERO);
                    return mapper.toEntity(line);
                })))
                .then();
    }

    @Override
    public Mono<PurchaseOrder> findDetail(Long id, Long companyId) {
        return databaseClient.sql(DETAIL_SQL)
                .bind("id", id)
                .bind("companyId", companyId)
                .map(row -> readHeader(row, false))
                .one()
                .flatMap(order -> databaseClient.sql(LINES_SQL)
                        .bind("id", id)
                        .map(this::readLine)
                        .all()
                        .collectList()
                        .map(lines -> {
                            order.setLines(lines);
                            return order;
                        }));
    }

    @Override
    public Flux<PurchaseOrder> findPage(Long companyId, List<Long> allowedWarehouseIds, PurchaseOrderStatus status,
                                        Long supplierId, String searchPattern, int page, int size) {
        return bindFilters(databaseClient.sql(pageSql(allowedWarehouseIds)), companyId, allowedWarehouseIds, status,
                supplierId, searchPattern)
                .bind("limit", size)
                .bind("offset", (long) page * size)
                .map(row -> readHeader(row, true))
                .all();
    }

    @Override
    public Mono<Long> count(Long companyId, List<Long> allowedWarehouseIds, PurchaseOrderStatus status,
                            Long supplierId, String searchPattern) {
        return bindFilters(databaseClient.sql(countSql(allowedWarehouseIds)), companyId, allowedWarehouseIds, status,
                supplierId, searchPattern)
                .map(row -> row.get("total_count", Long.class))
                .one();
    }

    @Override
    public Flux<PurchaseOrder> findArriving(Long companyId, List<Long> allowedWarehouseIds, int days, int limit) {
        return WarehouseScopeSql.bind(databaseClient.sql(arrivingSql(allowedWarehouseIds))
                        .bind("companyId", companyId)
                        .bind("days", days)
                        .bind("limit", limit), allowedWarehouseIds)
                .map(row -> readHeader(row, false))
                .all();
    }

    @Override
    public Mono<Long> countArriving(Long companyId, List<Long> allowedWarehouseIds, int days) {
        return WarehouseScopeSql.bind(databaseClient.sql(arrivingCountSql(allowedWarehouseIds))
                        .bind("companyId", companyId)
                        .bind("days", days), allowedWarehouseIds)
                .map(row -> row.get("total_count", Long.class))
                .one();
    }

    @Override
    public Mono<Long> countOverdue(Long companyId, List<Long> allowedWarehouseIds) {
        return WarehouseScopeSql.bind(databaseClient.sql(overdueCountSql(allowedWarehouseIds))
                        .bind("companyId", companyId), allowedWarehouseIds)
                .map(row -> row.get("total_count", Long.class))
                .one();
    }

    @Override
    public Mono<Void> addReceivedQuantity(Long lineId, BigDecimal quantity) {
        return lineRepository.addReceived(lineId, quantity).then();
    }

    @Override
    public Mono<Boolean> existsBySupplierId(Long supplierId) {
        return orderRepository.existsBySupplier(supplierId);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return orderRepository.deleteById(id);
    }

    private PurchaseOrder readHeader(Readable row, boolean withLineCount) {
        PurchaseOrder.PurchaseOrderBuilder builder = PurchaseOrder.builder()
                .id(row.get("id", Long.class))
                .companyId(row.get("company_id", Long.class))
                .supplierId(row.get("supplier_id", Long.class))
                .supplierName(row.get("supplier_name", String.class))
                .warehouseId(row.get("warehouse_id", Long.class))
                .warehouseName(row.get("warehouse_name", String.class))
                .number(row.get("number", String.class))
                .status(PurchaseOrderStatus.valueOf(row.get("status", String.class)))
                .expectedDate(row.get("expected_date", LocalDate.class))
                .notes(row.get("notes", String.class))
                .total(row.get("total", BigDecimal.class))
                .createdBy(row.get("created_by", Long.class))
                .createdByName(row.get("created_by_name", String.class))
                .orderedAt(row.get("ordered_at", LocalDateTime.class))
                .cancelledAt(row.get("cancelled_at", LocalDateTime.class))
                .createdAt(row.get("created_at", LocalDateTime.class))
                .updatedAt(row.get("updated_at", LocalDateTime.class));

        if (withLineCount) {
            Long count = row.get("line_count", Long.class);
            builder.lineCount(count == null ? 0 : count.intValue());
        }
        return builder.build();
    }

    private PurchaseOrderLine readLine(Readable row) {
        return PurchaseOrderLine.builder()
                .id(row.get("id", Long.class))
                .purchaseOrderId(row.get("purchase_order_id", Long.class))
                .productId(row.get("product_id", Long.class))
                .productName(row.get("product_name", String.class))
                .productSku(row.get("product_sku", String.class))
                .unit(UnitOfMeasure.valueOf(row.get("unit", String.class)))
                .tracksExpiry(row.get("tracks_expiry", Boolean.class))
                .quantity(row.get("quantity", BigDecimal.class))
                .unitCost(row.get("unit_cost", BigDecimal.class))
                .receivedQuantity(row.get("received_quantity", BigDecimal.class))
                .packaging(row.get("packaging", String.class))
                .entryInput(row.get("entry_input", String.class))
                .build();
    }

    /** Null filters must be bound with an explicit type, otherwise PostgreSQL can't infer the parameter type. */
    private DatabaseClient.GenericExecuteSpec bindFilters(DatabaseClient.GenericExecuteSpec spec, Long companyId,
                                                          List<Long> allowed, PurchaseOrderStatus status,
                                                          Long supplierId, String search) {
        spec = WarehouseScopeSql.bind(spec.bind("companyId", companyId), allowed);
        spec = status == null ? spec.bindNull("status", String.class) : spec.bind("status", status.name());
        spec = supplierId == null ? spec.bindNull("supplierId", Long.class) : spec.bind("supplierId", supplierId);
        spec = search == null ? spec.bindNull("search", String.class) : spec.bind("search", search);
        return spec;
    }
}
