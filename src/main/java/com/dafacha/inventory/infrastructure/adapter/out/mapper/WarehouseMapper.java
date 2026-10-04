package com.dafacha.inventory.infrastructure.adapter.out.mapper;

import com.dafacha.inventory.application.dto.response.WarehouseResponse;
import com.dafacha.inventory.domain.model.Warehouse;
import com.dafacha.inventory.infrastructure.adapter.out.entity.WarehouseEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {
    Warehouse toDomain(WarehouseEntity entity);
    WarehouseEntity toEntity(Warehouse domain);
    WarehouseResponse toResponse(Warehouse domain);
}
