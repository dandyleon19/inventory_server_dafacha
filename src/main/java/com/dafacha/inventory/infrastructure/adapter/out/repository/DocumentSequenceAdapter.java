package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.ports.out.DocumentSequencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class DocumentSequenceAdapter implements DocumentSequencePort {

    /** First call inserts 1; later calls add 1. One statement, so two concurrent requests never get the same number. */
    private static final String NEXT_SQL = """
            INSERT INTO document_sequences (company_id, name, last_value)
            VALUES (:companyId, :name, 1)
            ON CONFLICT (company_id, name) DO UPDATE
                SET last_value = document_sequences.last_value + 1
            RETURNING last_value
            """;

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Long> next(Long companyId, String sequenceName) {
        return databaseClient.sql(NEXT_SQL)
                .bind("companyId", companyId)
                .bind("name", sequenceName)
                .map(row -> row.get("last_value", Long.class))
                .one();
    }
}
