package com.dafacha.inventory.domain.ports.in;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.domain.model.Supplier;
import reactor.core.publisher.Mono;

public interface SupplierUseCase {
    Mono<Supplier> create(Supplier supplier);
    Mono<Supplier> update(Long id, Supplier changes);
    Mono<Void> delete(Long id);
    Mono<Supplier> findById(Long id);
    Mono<PageResponse<Supplier>> findAll(int page, int size, String search, Boolean isActive);
}
