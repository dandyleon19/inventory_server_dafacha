package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.model.AuditLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface AuditRepositoryPort {

    Mono<Void> save(AuditLog entry);

    /**
     * Newest first. {@code searchPattern} is an already-escaped ILIKE pattern matched against the label, the user's
     * name and the details; {@code from} is inclusive and {@code to} exclusive.
     */
    Flux<AuditLog> findPage(Long companyId, AuditEntity entityType, AuditAction action, Long userId,
                            String searchPattern, LocalDateTime from, LocalDateTime to, int page, int size);

    Mono<Long> count(Long companyId, AuditEntity entityType, AuditAction action, Long userId,
                     String searchPattern, LocalDateTime from, LocalDateTime to);
}
