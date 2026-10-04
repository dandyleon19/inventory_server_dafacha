package com.dafacha.inventory.infrastructure.adapter.out.mapper;

import com.dafacha.inventory.application.dto.response.CompanySettingsResponse;
import com.dafacha.inventory.domain.model.CompanySettings;
import com.dafacha.inventory.infrastructure.adapter.out.entity.CompanySettingsEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanySettingsMapper {
    CompanySettings toDomain(CompanySettingsEntity entity);
    CompanySettingsResponse toResponse(CompanySettings domain);
}
