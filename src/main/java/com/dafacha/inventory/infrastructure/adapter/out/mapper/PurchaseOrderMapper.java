package com.dafacha.inventory.infrastructure.adapter.out.mapper;

import com.dafacha.inventory.application.dto.response.PurchaseOrderLineResponse;
import com.dafacha.inventory.application.dto.response.PurchaseOrderResponse;
import com.dafacha.inventory.domain.model.PurchaseOrder;
import com.dafacha.inventory.domain.model.PurchaseOrderLine;
import com.dafacha.inventory.infrastructure.adapter.out.entity.PurchaseOrderEntity;
import com.dafacha.inventory.infrastructure.adapter.out.entity.PurchaseOrderLineEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PurchaseOrderMapper {
    PurchaseOrder toDomain(PurchaseOrderEntity entity);
    PurchaseOrderEntity toEntity(PurchaseOrder domain);

    PurchaseOrderLine toDomain(PurchaseOrderLineEntity entity);
    PurchaseOrderLineEntity toEntity(PurchaseOrderLine domain);

    /** pendingQuantity and lineTotal come from the line's computed getters; `lines` is mapped with the method below. */
    PurchaseOrderResponse toResponse(PurchaseOrder domain);
    PurchaseOrderLineResponse toResponse(PurchaseOrderLine domain);
}
