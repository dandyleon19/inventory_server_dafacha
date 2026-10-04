package com.dafacha.inventory.infrastructure.adapter.out.mapper;

import com.dafacha.inventory.application.dto.response.ProductResponse;
import com.dafacha.inventory.domain.model.Product;
import com.dafacha.inventory.infrastructure.adapter.out.entity.ProductEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toDomain(ProductEntity entity);
    ProductEntity toEntity(Product domain);

    /** marginAmount / marginPercent come from the domain's computed getters. */
    ProductResponse toResponse(Product domain);
}
