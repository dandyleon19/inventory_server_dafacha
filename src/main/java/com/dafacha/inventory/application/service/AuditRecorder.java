package com.dafacha.inventory.application.service;

import com.dafacha.inventory.application.security.SecurityHelper;
import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.model.AuditLog;
import com.dafacha.inventory.domain.model.User;
import com.dafacha.inventory.domain.ports.out.AuditRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Writes the audit trail. The use cases call it after the change has been made, in the same reactive chain,
 * and chain it like {@code .flatMap(saved -> audit.record(...).thenReturn(saved))}.
 *
 * It never fails the operation it describes: if the trail can't be written the error is logged and swallowed.
 */
@Slf4j
@RequiredArgsConstructor
public class AuditRecorder {

    private static final int MAX_LABEL = 250;

    private final AuditRepositoryPort auditRepositoryPort;

    /** Records an action by the current caller. Does nothing when {@code details} is blank for an UPDATE. */
    public Mono<Void> record(AuditAction action, AuditEntity entity, Long entityId, String label, String details) {
        if (action == AuditAction.UPDATE && (details == null || details.isBlank())) {
            return Mono.empty(); // nothing actually changed
        }

        return SecurityHelper.currentUser()
                .flatMap(caller -> save(AuditLog.builder()
                        .companyId(caller.getCompanyId())
                        .userId(caller.getUserId())
                        .userName(caller.getUserName())
                        .action(action)
                        .entityType(entity)
                        .entityId(entityId)
                        .entityLabel(trim(label))
                        .details(details == null || details.isBlank() ? null : details)
                        .build()));
    }

    /** A login happens before there is an authenticated caller, so the user is passed in. */
    public Mono<Void> recordLogin(User user) {
        if (user.getCompanyId() == null) {
            return Mono.empty();
        }
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return save(AuditLog.builder()
                .companyId(user.getCompanyId())
                .userId(user.getId())
                .userName(name)
                .action(AuditAction.LOGIN)
                .entityType(AuditEntity.SESSION)
                .entityId(user.getId())
                .entityLabel(trim(user.getEmail()))
                .build());
    }

    private Mono<Void> save(AuditLog entry) {
        return auditRepositoryPort.save(entry)
                .onErrorResume(error -> {
                    log.warn("Could not write the audit entry {} {} ({}): {}",
                            entry.getAction(), entry.getEntityType(), entry.getEntityLabel(), error.getMessage());
                    return Mono.empty();
                });
    }

    private static String trim(String label) {
        if (label == null) {
            return null;
        }
        return label.length() <= MAX_LABEL ? label : label.substring(0, MAX_LABEL);
    }
}
