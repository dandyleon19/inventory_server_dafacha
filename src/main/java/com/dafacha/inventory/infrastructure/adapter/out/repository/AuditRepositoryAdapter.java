package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.model.AuditLog;
import com.dafacha.inventory.domain.ports.out.AuditRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** The audit log is append-only and filtered by hand-written SQL, so it uses DatabaseClient directly. */
@RequiredArgsConstructor
public class AuditRepositoryAdapter implements AuditRepositoryPort {

    private static final String INSERT_SQL = """
            INSERT INTO audit_log (company_id, user_id, user_name, action, entity_type, entity_id, entity_label, details)
            VALUES (:companyId, :userId, :userName, :action, :entityType, :entityId, :entityLabel, :details)
            """;

    private static final String FILTERS = """
            FROM audit_log
            WHERE company_id = :companyId
              AND (CAST(:entityType AS TEXT) IS NULL OR entity_type = :entityType)
              AND (CAST(:action AS TEXT) IS NULL OR action = :action)
              AND (CAST(:userId AS BIGINT) IS NULL OR user_id = :userId)
              AND (
                  CAST(:search AS TEXT) IS NULL
                  OR entity_label ILIKE :search
                  OR user_name ILIKE :search
                  OR details ILIKE :search
              )
              AND (CAST(:from AS TIMESTAMP) IS NULL OR created_at >= :from)
              AND (CAST(:to AS TIMESTAMP) IS NULL OR created_at < :to)
            """;

    private static final String PAGE_SQL = """
            SELECT id, company_id, user_id, user_name, action, entity_type, entity_id, entity_label, details, created_at
            """ + FILTERS + """
            ORDER BY created_at DESC, id DESC
            LIMIT :limit OFFSET :offset
            """;

    private static final String COUNT_SQL = "SELECT COUNT(*) AS total " + FILTERS;

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Void> save(AuditLog entry) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(INSERT_SQL)
                .bind("companyId", entry.getCompanyId())
                .bind("action", entry.getAction().name())
                .bind("entityType", entry.getEntityType().name());
        spec = bindOrNull(spec, "userId", entry.getUserId(), Long.class);
        spec = bindOrNull(spec, "userName", entry.getUserName(), String.class);
        spec = bindOrNull(spec, "entityId", entry.getEntityId(), Long.class);
        spec = bindOrNull(spec, "entityLabel", entry.getEntityLabel(), String.class);
        spec = bindOrNull(spec, "details", entry.getDetails(), String.class);
        return spec.fetch().rowsUpdated().then();
    }

    @Override
    public Flux<AuditLog> findPage(Long companyId, AuditEntity entityType, AuditAction action, Long userId,
                                   String searchPattern, LocalDateTime from, LocalDateTime to, int page, int size) {
        return bindFilters(databaseClient.sql(PAGE_SQL), companyId, entityType, action, userId, searchPattern, from, to)
                .bind("limit", size)
                .bind("offset", (long) page * size)
                .map(row -> AuditLog.builder()
                        .id(row.get("id", Long.class))
                        .companyId(row.get("company_id", Long.class))
                        .userId(row.get("user_id", Long.class))
                        .userName(row.get("user_name", String.class))
                        .action(AuditAction.valueOf(row.get("action", String.class)))
                        .entityType(AuditEntity.valueOf(row.get("entity_type", String.class)))
                        .entityId(row.get("entity_id", Long.class))
                        .entityLabel(row.get("entity_label", String.class))
                        .details(row.get("details", String.class))
                        .createdAt(row.get("created_at", LocalDateTime.class))
                        .build())
                .all();
    }

    @Override
    public Mono<Long> count(Long companyId, AuditEntity entityType, AuditAction action, Long userId,
                            String searchPattern, LocalDateTime from, LocalDateTime to) {
        return bindFilters(databaseClient.sql(COUNT_SQL), companyId, entityType, action, userId, searchPattern, from, to)
                .map(row -> row.get("total", Long.class))
                .one();
    }

    /** Null filters must be bound with an explicit type, otherwise PostgreSQL can't infer the parameter type. */
    private DatabaseClient.GenericExecuteSpec bindFilters(DatabaseClient.GenericExecuteSpec spec, Long companyId,
                                                          AuditEntity entityType, AuditAction action, Long userId,
                                                          String search, LocalDateTime from, LocalDateTime to) {
        spec = spec.bind("companyId", companyId);
        spec = bindOrNull(spec, "entityType", entityType == null ? null : entityType.name(), String.class);
        spec = bindOrNull(spec, "action", action == null ? null : action.name(), String.class);
        spec = bindOrNull(spec, "userId", userId, Long.class);
        spec = bindOrNull(spec, "search", search, String.class);
        spec = bindOrNull(spec, "from", from, LocalDateTime.class);
        spec = bindOrNull(spec, "to", to, LocalDateTime.class);
        return spec;
    }

    private static <T> DatabaseClient.GenericExecuteSpec bindOrNull(DatabaseClient.GenericExecuteSpec spec, String name,
                                                                    T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }
}
