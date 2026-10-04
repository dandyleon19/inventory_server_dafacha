package com.dafacha.inventory.infrastructure.adapter.in;

import com.dafacha.inventory.application.dto.response.AuditLogResponse;
import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.ports.in.AuditUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/** Read-only: the audit trail is written by the use cases and can't be edited. Admins only (see SecurityConfig). */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditUseCase auditUseCase;

    @GetMapping
    public Mono<ResponseEntity<PageResponse<AuditLogResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AuditEntity entityType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return auditUseCase.findAll(page, size, entityType, action, userId, search, from, to)
                .map(result -> result.map(AuditLogResponse::from))
                .map(ResponseEntity::ok);
    }
}
