package com.dafacha.inventory.infrastructure.adapter.out.mapper;

import com.dafacha.inventory.application.dto.response.InventoryValuationResponse;
import com.dafacha.inventory.application.dto.response.StockLotResponse;
import com.dafacha.inventory.application.dto.response.StockSummaryResponse;
import com.dafacha.inventory.application.dto.response.WarehouseValuationResponse;
import com.dafacha.inventory.domain.model.InventoryValuation;
import com.dafacha.inventory.domain.model.StockLot;
import com.dafacha.inventory.domain.model.StockSummary;
import com.dafacha.inventory.domain.model.WarehouseValuation;
import org.mapstruct.Mapper;

/** Response mapping for the read models (stock summary and valuation); they have no persistence entity. */
@Mapper(componentModel = "spring")
public interface StockMapper {

    /** value and lowStock come from StockSummary's computed getters. */
    StockSummaryResponse toResponse(StockSummary domain);

    StockLotResponse toResponse(StockLot domain);

    WarehouseValuationResponse toResponse(WarehouseValuation domain);

    InventoryValuationResponse toResponse(InventoryValuation domain);
}
