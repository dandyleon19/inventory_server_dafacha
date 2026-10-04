package com.dafacha.inventory.infrastructure.adapter.out.mapper;

import com.dafacha.inventory.application.dto.response.StockMovementResponse;
import com.dafacha.inventory.domain.model.StockMovement;
import com.dafacha.inventory.infrastructure.adapter.out.entity.StockMovementEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {
    StockMovement toDomain(StockMovementEntity entity);
    StockMovementEntity toEntity(StockMovement domain);
    StockMovementResponse toResponse(StockMovement domain);
}
