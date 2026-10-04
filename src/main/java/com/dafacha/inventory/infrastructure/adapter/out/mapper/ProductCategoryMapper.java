package com.dafacha.inventory.infrastructure.adapter.out.mapper;

import com.dafacha.inventory.application.dto.response.ProductCategoryResponse;
import com.dafacha.inventory.domain.model.ProductCategory;
import com.dafacha.inventory.infrastructure.adapter.out.entity.ProductCategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductCategoryMapper {
    ProductCategory toDomain(ProductCategoryEntity entity);
    ProductCategoryEntity toEntity(ProductCategory domain);
    ProductCategoryResponse toResponse(ProductCategory domain);
}
