package com.dafacha.inventory.domain.ports.in;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.model.AuditLog;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface AuditUseCase {

    /** The company's audit trail, newest first. {@code from} / {@code to} are inclusive calendar days. */
    Mono<PageResponse<AuditLog>> findAll(int page, int size, AuditEntity entityType, AuditAction action, Long userId,
                                         String search, LocalDate from, LocalDate to);
}
