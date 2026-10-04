package com.dafacha.inventory.infrastructure.adapter.out.mapper;

import com.dafacha.inventory.application.dto.response.CompanyResponse;
import com.dafacha.inventory.domain.model.Company;
import com.dafacha.inventory.infrastructure.adapter.out.entity.CompanyEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyMapper {
    Company toDomain(CompanyEntity entity);
    CompanyEntity toEntity(Company domain);
    CompanyResponse toResponse(Company domain);
}
