package com.dafacha.inventory.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {
    private Long id;
    private Long companyId;
    private Long userId;
    /** Copied at the time of the action. */
    private String userName;
    private AuditAction action;
    private AuditEntity entityType;
    private Long entityId;
    /** Human name of what was touched (product name, order number...). */
    private String entityLabel;
    /** What changed, one line per field. */
    private String details;
    private LocalDateTime createdAt;
}
