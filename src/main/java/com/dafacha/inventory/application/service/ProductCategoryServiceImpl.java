package com.dafacha.inventory.application.service;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.security.SecurityHelper;
import com.dafacha.inventory.application.support.SearchSupport;
import com.dafacha.inventory.application.support.AuditDiff;
import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.exception.ConflictException;
import com.dafacha.inventory.domain.exception.NotFoundException;
import com.dafacha.inventory.domain.model.ProductCategory;
import com.dafacha.inventory.domain.ports.in.ProductCategoryUseCase;
import com.dafacha.inventory.domain.ports.out.ProductCategoryRepositoryPort;
import com.dafacha.inventory.domain.ports.out.ProductRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryUseCase {

    private static final String RESOURCE = "Product category";

    private final ProductCategoryRepositoryPort categoryRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final AuditRecorder audit;

    @Override
    public Mono<ProductCategory> create(ProductCategory category) {
        return doCreate(category).flatMap(created -> audit.record(AuditAction.CREATE, AuditEntity.CATEGORY,
                        created.getId(), created.getName(), AuditDiff.describe(snapshot(created)))
                .thenReturn(created));
    }

    @Override
    public Mono<ProductCategory> update(Long id, ProductCategory changes) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(existing -> {
                    Map<String, Object> before = snapshot(existing);
                    return doUpdate(id, changes).flatMap(updated -> audit.record(AuditAction.UPDATE,
                                    AuditEntity.CATEGORY, id, updated.getName(), AuditDiff.between(before, snapshot(updated)))
                            .thenReturn(updated));
                }));
    }

    @Override
    public Mono<Void> delete(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(existing -> doDelete(id)
                        .then(Mono.defer(() -> audit.record(AuditAction.DELETE, AuditEntity.CATEGORY, id,
                                existing.getName(), AuditDiff.describe(snapshot(existing)))))));
    }

    private static Map<String, Object> snapshot(ProductCategory category) {
        Map<String, Object> snapshot = AuditDiff.snapshot();
        snapshot.put("Nombre", category.getName());
        snapshot.put("Descripción", category.getDescription());
        return snapshot;
    }

    private Mono<ProductCategory> doCreate(ProductCategory category) {
        return SecurityHelper.currentCompanyId().flatMap(companyId -> {
            normalize(category);
            category.setCompanyId(companyId);
            return validateUniqueName(companyId, category.getName(), null)
                    .then(Mono.defer(() -> categoryRepositoryPort.save(category)));
        });
    }

    private Mono<ProductCategory> doUpdate(Long id, ProductCategory changes) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(existing -> {
                    normalize(changes);
                    existing.setName(changes.getName());
                    existing.setDescription(changes.getDescription());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return validateUniqueName(companyId, existing.getName(), id)
                            .then(Mono.defer(() -> categoryRepositoryPort.save(existing)));
                }));
    }

    private Mono<Void> doDelete(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId)
                        .flatMap(existing -> productRepositoryPort.countByCategoryId(companyId, id))
                        .flatMap(productCount -> {
                            if (productCount > 0) {
                                return Mono.<Void>error(new ConflictException(
                                        "La categoría tiene " + productCount + " producto(s); reasígnalos o elimínalos primero"));
                            }
                            return categoryRepositoryPort.deleteById(id);
                        }));
    }

    @Override
    public Mono<ProductCategory> findById(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId -> find(id, companyId));
    }

    @Override
    public Mono<PageResponse<ProductCategory>> findAll(int page, int size, String search) {
        int safePage = SearchSupport.safePage(page);
        int safeSize = SearchSupport.safeSize(size);
        String pattern = SearchSupport.likePattern(search);

        return SecurityHelper.currentCompanyId().flatMap(companyId -> Mono.zip(
                        categoryRepositoryPort.findPage(companyId, pattern, safePage, safeSize).collectList(),
                        categoryRepositoryPort.count(companyId, pattern))
                .map(tuple -> PageResponse.of(tuple.getT1(), safePage, safeSize, tuple.getT2())));
    }

    private Mono<ProductCategory> find(Long id, Long companyId) {
        return categoryRepositoryPort.findByIdAndCompanyId(id, companyId)
                .switchIfEmpty(Mono.error(NotFoundException.forResource(RESOURCE, id)));
    }

    private Mono<Void> validateUniqueName(Long companyId, String name, Long excludeId) {
        return categoryRepositoryPort.existsByName(companyId, name, excludeId)
                .filter(Boolean::booleanValue)
                .flatMap(exists -> Mono.<Void>error(
                        new ConflictException("Ya existe una categoría llamada '" + name + "'")));
    }

    private void normalize(ProductCategory category) {
        category.setName(category.getName() == null ? null : category.getName().trim());
        category.setDescription(SearchSupport.blankToNull(category.getDescription()));
    }
}
