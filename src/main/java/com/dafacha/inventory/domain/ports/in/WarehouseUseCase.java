package com.dafacha.inventory.domain.ports.in;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.domain.model.Warehouse;
import reactor.core.publisher.Mono;

public interface WarehouseUseCase {
    Mono<Warehouse> create(Warehouse warehouse);
    Mono<Warehouse> update(Long id, Warehouse changes);
    Mono<Void> delete(Long id);
    Mono<Warehouse> findById(Long id);
    Mono<PageResponse<Warehouse>> findAll(int page, int size, String search, Boolean isActive);
}
