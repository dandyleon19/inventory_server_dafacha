package com.dafacha.inventory.application.service;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.security.SecurityHelper;
import com.dafacha.inventory.application.support.SearchSupport;
import com.dafacha.inventory.application.support.AuditDiff;
import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.exception.ConflictException;
import com.dafacha.inventory.domain.exception.NotFoundException;
import com.dafacha.inventory.domain.model.Warehouse;
import com.dafacha.inventory.domain.ports.in.WarehouseUseCase;
import com.dafacha.inventory.domain.ports.out.StockMovementRepositoryPort;
import com.dafacha.inventory.domain.ports.out.WarehouseRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseUseCase {

    private static final String RESOURCE = "Warehouse";

    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final StockMovementRepositoryPort movementRepositoryPort;
    private final AuditRecorder audit;

    @Override
    public Mono<Warehouse> create(Warehouse warehouse) {
        return doCreate(warehouse).flatMap(created -> audit.record(AuditAction.CREATE, AuditEntity.WAREHOUSE,
                        created.getId(), created.getName(), AuditDiff.describe(snapshot(created)))
                .thenReturn(created));
    }

    @Override
    public Mono<Warehouse> update(Long id, Warehouse changes) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(existing -> {
                    Map<String, Object> before = snapshot(existing);
                    return doUpdate(id, changes).flatMap(updated -> audit.record(AuditAction.UPDATE,
                                    AuditEntity.WAREHOUSE, id, updated.getName(), AuditDiff.between(before, snapshot(updated)))
                            .thenReturn(updated));
                }));
    }

    @Override
    public Mono<Void> delete(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(existing -> doDelete(id)
                        .then(Mono.defer(() -> audit.record(AuditAction.DELETE, AuditEntity.WAREHOUSE, id,
                                existing.getName(), AuditDiff.describe(snapshot(existing)))))));
    }

    private static Map<String, Object> snapshot(Warehouse warehouse) {
        Map<String, Object> snapshot = AuditDiff.snapshot();
        snapshot.put("Nombre", warehouse.getName());
        snapshot.put("Dirección", warehouse.getAddress());
        snapshot.put("Activo", warehouse.getIsActive());
        return snapshot;
    }

    private Mono<Warehouse> doCreate(Warehouse warehouse) {
        return SecurityHelper.currentCompanyId().flatMap(companyId -> {
            normalize(warehouse);
            warehouse.setCompanyId(companyId);
            if (warehouse.getIsActive() == null) {
                warehouse.setIsActive(true);
            }
            return validateUniqueName(companyId, warehouse.getName(), null)
                    .then(Mono.defer(() -> warehouseRepositoryPort.save(warehouse)));
        });
    }

    private Mono<Warehouse> doUpdate(Long id, Warehouse changes) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(existing -> {
                    normalize(changes);
                    existing.setName(changes.getName());
                    existing.setAddress(changes.getAddress());
                    if (changes.getIsActive() != null) {
                        existing.setIsActive(changes.getIsActive());
                    }
                    existing.setUpdatedAt(LocalDateTime.now());
                    return validateUniqueName(companyId, existing.getName(), id)
                            .then(Mono.defer(() -> warehouseRepositoryPort.save(existing)));
                }));
    }

    /** A warehouse with any stock history can't be deleted (its movements would lose their place); deactivate it. */
    private Mono<Void> doDelete(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId)
                        .flatMap(existing -> movementRepositoryPort.existsByWarehouseId(id))
                        .flatMap(hasHistory -> {
                            if (hasHistory) {
                                return Mono.<Void>error(new ConflictException(
                                        "El almacén tiene movimientos de stock; desactívalo en lugar de eliminarlo"));
                            }
                            return warehouseRepositoryPort.deleteById(id);
                        }));
    }

    @Override
    public Mono<Warehouse> findById(Long id) {
        return SecurityHelper.currentTenantUser().flatMap(user -> find(id, user.getCompanyId())
                .flatMap(warehouse -> user.canAccessWarehouse(id)
                        ? Mono.just(warehouse)
                        : Mono.<Warehouse>error(NotFoundException.forResource(RESOURCE, id))));
    }

    @Override
    public Mono<PageResponse<Warehouse>> findAll(int page, int size, String search, Boolean isActive) {
        int safePage = SearchSupport.safePage(page);
        int safeSize = SearchSupport.safeSize(size);
        String pattern = SearchSupport.likePattern(search);

        return SecurityHelper.currentTenantUser().flatMap(user -> {
            Long companyId = user.getCompanyId();
            if (!user.isWarehouseRestricted()) {
                return Mono.zip(
                                warehouseRepositoryPort.findPage(companyId, isActive, pattern, safePage, safeSize).collectList(),
                                warehouseRepositoryPort.count(companyId, isActive, pattern))
                        .map(tuple -> PageResponse.of(tuple.getT1(), safePage, safeSize, tuple.getT2()));
            }

            // A user limited to some warehouses sees only those. A company has a handful of warehouses, so the
            // list is filtered in memory and then paged, instead of complicating the query.
            return warehouseRepositoryPort.findPage(companyId, isActive, pattern, 0, SearchSupport.MAX_PAGE_SIZE * 10)
                    .filter(warehouse -> user.canAccessWarehouse(warehouse.getId()))
                    .collectList()
                    .map(allowed -> {
                        int from = Math.min(safePage * safeSize, allowed.size());
                        int to = Math.min(from + safeSize, allowed.size());
                        List<Warehouse> content = allowed.subList(from, to);
                        return PageResponse.of(content, safePage, safeSize, allowed.size());
                    });
        });
    }

    private Mono<Warehouse> find(Long id, Long companyId) {
        return warehouseRepositoryPort.findByIdAndCompanyId(id, companyId)
                .switchIfEmpty(Mono.error(NotFoundException.forResource(RESOURCE, id)));
    }

    private Mono<Void> validateUniqueName(Long companyId, String name, Long excludeId) {
        return warehouseRepositoryPort.existsByName(companyId, name, excludeId)
                .filter(Boolean::booleanValue)
                .flatMap(exists -> Mono.<Void>error(
                        new ConflictException("Ya existe un almacén llamado '" + name + "'")));
    }

    private void normalize(Warehouse warehouse) {
        warehouse.setName(warehouse.getName() == null ? null : warehouse.getName().trim());
        warehouse.setAddress(SearchSupport.blankToNull(warehouse.getAddress()));
    }
}
