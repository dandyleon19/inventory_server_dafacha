package com.dafacha.inventory.application.service;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.security.SecurityHelper;
import com.dafacha.inventory.application.support.SearchSupport;
import com.dafacha.inventory.application.support.AuditDiff;
import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.exception.ConflictException;
import com.dafacha.inventory.domain.exception.NotFoundException;
import com.dafacha.inventory.domain.model.Supplier;
import com.dafacha.inventory.domain.ports.in.SupplierUseCase;
import com.dafacha.inventory.domain.ports.out.PurchaseOrderRepositoryPort;
import com.dafacha.inventory.domain.ports.out.SupplierRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierUseCase {

    private static final String RESOURCE = "Supplier";

    private final SupplierRepositoryPort supplierRepositoryPort;
    private final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;
    private final AuditRecorder audit;

    @Override
    public Mono<Supplier> create(Supplier supplier) {
        return doCreate(supplier).flatMap(created -> audit.record(AuditAction.CREATE, AuditEntity.SUPPLIER,
                        created.getId(), created.getName(), AuditDiff.describe(snapshot(created)))
                .thenReturn(created));
    }

    @Override
    public Mono<Supplier> update(Long id, Supplier changes) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(existing -> {
                    Map<String, Object> before = snapshot(existing);
                    return doUpdate(id, changes).flatMap(updated -> audit.record(AuditAction.UPDATE,
                                    AuditEntity.SUPPLIER, id, updated.getName(), AuditDiff.between(before, snapshot(updated)))
                            .thenReturn(updated));
                }));
    }

    @Override
    public Mono<Void> delete(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(existing -> doDelete(id)
                        .then(Mono.defer(() -> audit.record(AuditAction.DELETE, AuditEntity.SUPPLIER, id,
                                existing.getName(), AuditDiff.describe(snapshot(existing)))))));
    }

    private static Map<String, Object> snapshot(Supplier supplier) {
        Map<String, Object> snapshot = AuditDiff.snapshot();
        snapshot.put("Nombre", supplier.getName());
        snapshot.put("RUC", supplier.getRucNumber());
        snapshot.put("Contacto", supplier.getContactName());
        snapshot.put("Teléfono", supplier.getPhone());
        snapshot.put("Correo", supplier.getEmail());
        snapshot.put("Dirección", supplier.getAddress());
        snapshot.put("Notas", supplier.getNotes());
        snapshot.put("Activo", supplier.getIsActive());
        return snapshot;
    }

    private Mono<Supplier> doCreate(Supplier supplier) {
        return SecurityHelper.currentCompanyId().flatMap(companyId -> {
            normalize(supplier);
            supplier.setCompanyId(companyId);
            if (supplier.getIsActive() == null) {
                supplier.setIsActive(true);
            }
            return validateUnique(companyId, supplier, null)
                    .then(Mono.defer(() -> supplierRepositoryPort.save(supplier)));
        });
    }

    private Mono<Supplier> doUpdate(Long id, Supplier changes) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(existing -> {
                    normalize(changes);
                    existing.setName(changes.getName());
                    existing.setRucNumber(changes.getRucNumber());
                    existing.setContactName(changes.getContactName());
                    existing.setPhone(changes.getPhone());
                    existing.setEmail(changes.getEmail());
                    existing.setAddress(changes.getAddress());
                    existing.setNotes(changes.getNotes());
                    if (changes.getIsActive() != null) {
                        existing.setIsActive(changes.getIsActive());
                    }
                    existing.setUpdatedAt(LocalDateTime.now());
                    return validateUnique(companyId, existing, id)
                            .then(Mono.defer(() -> supplierRepositoryPort.save(existing)));
                }));
    }

    /** A supplier that has purchase orders can't be deleted (they would lose their supplier); deactivate it. */
    private Mono<Void> doDelete(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId)
                        .flatMap(existing -> purchaseOrderRepositoryPort.existsBySupplierId(id))
                        .flatMap(hasOrders -> {
                            if (hasOrders) {
                                return Mono.<Void>error(new ConflictException(
                                        "El proveedor tiene órdenes de compra; desactívalo en lugar de eliminarlo"));
                            }
                            return supplierRepositoryPort.deleteById(id);
                        }));
    }

    @Override
    public Mono<Supplier> findById(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId -> find(id, companyId));
    }

    @Override
    public Mono<PageResponse<Supplier>> findAll(int page, int size, String search, Boolean isActive) {
        int safePage = SearchSupport.safePage(page);
        int safeSize = SearchSupport.safeSize(size);
        String pattern = SearchSupport.likePattern(search);

        return SecurityHelper.currentCompanyId().flatMap(companyId -> Mono.zip(
                        supplierRepositoryPort.findPage(companyId, isActive, pattern, safePage, safeSize).collectList(),
                        supplierRepositoryPort.count(companyId, isActive, pattern))
                .map(tuple -> PageResponse.of(tuple.getT1(), safePage, safeSize, tuple.getT2())));
    }

    private Mono<Supplier> find(Long id, Long companyId) {
        return supplierRepositoryPort.findByIdAndCompanyId(id, companyId)
                .switchIfEmpty(Mono.error(NotFoundException.forResource(RESOURCE, id)));
    }

    private Mono<Void> validateUnique(Long companyId, Supplier supplier, Long excludeId) {
        Mono<Void> nameCheck = supplierRepositoryPort.existsByName(companyId, supplier.getName(), excludeId)
                .filter(Boolean::booleanValue)
                .flatMap(exists -> Mono.<Void>error(
                        new ConflictException("Ya existe un proveedor llamado '" + supplier.getName() + "'")));

        Mono<Void> rucCheck = supplier.getRucNumber() == null
                ? Mono.empty()
                : supplierRepositoryPort.existsByRucNumber(companyId, supplier.getRucNumber(), excludeId)
                        .filter(Boolean::booleanValue)
                        .flatMap(exists -> Mono.<Void>error(new ConflictException(
                                "Ya existe un proveedor con el RUC '" + supplier.getRucNumber() + "'")));

        return nameCheck.then(rucCheck);
    }

    private void normalize(Supplier supplier) {
        supplier.setName(supplier.getName() == null ? null : supplier.getName().trim());
        supplier.setRucNumber(SearchSupport.blankToNull(supplier.getRucNumber()));
        supplier.setContactName(SearchSupport.blankToNull(supplier.getContactName()));
        supplier.setPhone(SearchSupport.blankToNull(supplier.getPhone()));
        supplier.setEmail(SearchSupport.blankToNull(supplier.getEmail()));
        supplier.setAddress(SearchSupport.blankToNull(supplier.getAddress()));
        supplier.setNotes(SearchSupport.blankToNull(supplier.getNotes()));
    }
}
