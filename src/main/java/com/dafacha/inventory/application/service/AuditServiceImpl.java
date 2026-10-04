package com.dafacha.inventory.application.service;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.security.SecurityHelper;
import com.dafacha.inventory.application.support.SearchSupport;
import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.model.AuditLog;
import com.dafacha.inventory.domain.ports.in.AuditUseCase;
import com.dafacha.inventory.domain.ports.out.AuditRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class AuditServiceImpl implements AuditUseCase {

    private final AuditRepositoryPort auditRepositoryPort;

    @Override
    public Mono<PageResponse<AuditLog>> findAll(int page, int size, AuditEntity entityType, AuditAction action,
                                                Long userId, String search, LocalDate from, LocalDate to) {
        int safePage = SearchSupport.safePage(page);
        int safeSize = SearchSupport.safeSize(size);
        String pattern = SearchSupport.likePattern(search);
        LocalDateTime fromTime = from == null ? null : from.atStartOfDay();
        // "to" is a calendar day and includes all of it, so the bound is the start of the next day (exclusive).
        LocalDateTime toTime = to == null ? null : to.plusDays(1).atStartOfDay();

        return SecurityHelper.currentCompanyId().flatMap(companyId -> Mono.zip(
                        auditRepositoryPort.findPage(companyId, entityType, action, userId, pattern, fromTime, toTime,
                                safePage, safeSize).collectList(),
                        auditRepositoryPort.count(companyId, entityType, action, userId, pattern, fromTime, toTime))
                .map(tuple -> PageResponse.of(tuple.getT1(), safePage, safeSize, tuple.getT2())));
    }
}
