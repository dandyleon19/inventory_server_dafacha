package com.dafacha.inventory.infrastructure.adapter.out.mapper;

import com.dafacha.inventory.application.dto.response.UserResponse;
import com.dafacha.inventory.domain.model.User;
import com.dafacha.inventory.infrastructure.adapter.out.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toDomain(UserEntity entity);
    UserEntity toEntity(User domain);

    @Mapping(
            target = "fullName",
            expression = "java(domain.getFirstName() + \" \" + domain.getLastName())"
    )
    @Mapping(target = "companyName", source = "company.name")
    UserResponse toResponse(User domain);
}
