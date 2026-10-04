package com.dafacha.inventory.infrastructure.adapter.out.mapper;

import com.dafacha.inventory.domain.model.RefreshToken;
import com.dafacha.inventory.infrastructure.adapter.out.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {

    RefreshToken toDomain(RefreshTokenEntity entity);

    RefreshTokenEntity toEntity(RefreshToken domain);
}
