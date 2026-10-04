package com.dafacha.inventory.application.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record DashboardResponse(
        BigDecimal totalValue,
        Long activeProductCount,
        Long warehouseCount,
        Long lowStockCount,
        Long expiredLotCount,
        Long expiringLotCount,
        Long arrivingOrderCount,
        Long overdueOrderCount,
        Integer expiryAlertDays,
        Integer purchaseArrivalAlertDays,
        List<WarehouseValuationResponse> warehouses,
        List<StockSummaryResponse> lowStock,
        List<StockLotResponse> expiringLots,
        List<PurchaseOrderResponse> arrivingOrders,
        List<StockMovementResponse> recentMovements
) {
}
