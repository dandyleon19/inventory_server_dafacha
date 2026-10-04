package com.dafacha.inventory.application.dto.response;

import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.model.AuditLog;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AuditLogResponse(
        Long id,
        Long userId,
        String userName,
        AuditAction action,
        AuditEntity entityType,
        Long entityId,
        String entityLabel,
        String details,
        LocalDateTime createdAt
) {

    public static AuditLogResponse from(AuditLog entry) {
        return AuditLogResponse.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .userName(entry.getUserName())
                .action(entry.getAction())
                .entityType(entry.getEntityType())
                .entityId(entry.getEntityId())
                .entityLabel(entry.getEntityLabel())
                .details(entry.getDetails())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
