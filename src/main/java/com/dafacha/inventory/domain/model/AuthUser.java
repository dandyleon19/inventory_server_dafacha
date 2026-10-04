package com.dafacha.inventory.domain.model;

import lombok.*;

import java.util.List;

/** The caller as seen by the use cases. Loaded from the database on each request (see UserAccessResolver). */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthUser {
    private Long userId;
    private Long companyId;
    private String role;
    private String userName;
    /** The only warehouses this user may use; null means all of them. */
    private List<Long> warehouseIds;

    public boolean isWarehouseRestricted() {
        return warehouseIds != null;
    }

    public boolean canAccessWarehouse(Long warehouseId) {
        return warehouseIds == null || warehouseIds.contains(warehouseId);
    }
}
