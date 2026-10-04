package com.dafacha.inventory.infrastructure.adapter.out.mapper;

import com.dafacha.inventory.application.dto.response.SupplierResponse;
import com.dafacha.inventory.domain.model.Supplier;
import com.dafacha.inventory.infrastructure.adapter.out.entity.SupplierEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupplierMapper {
    Supplier toDomain(SupplierEntity entity);
    SupplierEntity toEntity(Supplier domain);
    SupplierResponse toResponse(Supplier domain);
}
