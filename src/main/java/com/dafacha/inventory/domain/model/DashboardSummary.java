package com.dafacha.inventory.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Everything the dashboard (and the alerts bell) shows, in one object. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummary {
    private BigDecimal totalValue;
    private Long activeProductCount;
    private Long warehouseCount;

    // Alert counters.
    private Long lowStockCount;
    private Long expiredLotCount;
    private Long expiringLotCount;
    private Long arrivingOrderCount;
    private Long overdueOrderCount;

    /** The windows the counters were computed with (company settings). */
    private Integer expiryAlertDays;
    private Integer purchaseArrivalAlertDays;

    // Short lists for the dashboard widgets.
    private List<WarehouseValuation> warehouses;
    private List<StockSummary> lowStock;
    private List<StockLot> expiringLots;
    private List<PurchaseOrder> arrivingOrders;
    private List<StockMovement> recentMovements;
}
