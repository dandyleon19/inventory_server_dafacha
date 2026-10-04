package com.dafacha.inventory.application.service;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.security.SecurityHelper;
import com.dafacha.inventory.application.support.SearchSupport;
import com.dafacha.inventory.application.support.AuditDiff;
import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.exception.BadRequestException;
import com.dafacha.inventory.domain.exception.ConflictException;
import com.dafacha.inventory.domain.exception.NotFoundException;
import com.dafacha.inventory.domain.model.CompanySettings;
import com.dafacha.inventory.domain.model.Product;
import com.dafacha.inventory.domain.model.ProductCategory;
import com.dafacha.inventory.domain.ports.in.ProductUseCase;
import com.dafacha.inventory.domain.ports.out.CompanySettingsRepositoryPort;
import com.dafacha.inventory.domain.ports.out.ProductCategoryRepositoryPort;
import com.dafacha.inventory.domain.ports.out.ProductRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockLotRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockMovementRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ProductServiceImpl implements ProductUseCase {

    private static final String RESOURCE = "Product";

    private final ProductRepositoryPort productRepositoryPort;
    private final ProductCategoryRepositoryPort categoryRepositoryPort;
    private final CompanySettingsRepositoryPort settingsRepositoryPort;
    private final StockMovementRepositoryPort movementRepositoryPort;
    private final StockLotRepositoryPort lotRepositoryPort;
    private final StockRepositoryPort stockRepositoryPort;
    private final AuditRecorder audit;

    @Override
    public Mono<Product> create(Product product) {
        return doCreate(product).flatMap(created -> audit.record(AuditAction.CREATE, AuditEntity.PRODUCT,
                        created.getId(), created.getName(), AuditDiff.describe(snapshot(created)))
                .thenReturn(created));
    }

    @Override
    public Mono<Product> update(Long id, Product changes) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(this::withCategoryName).flatMap(existing -> {
                    Map<String, Object> before = snapshot(existing);
                    return doUpdate(id, changes).flatMap(updated -> audit.record(AuditAction.UPDATE,
                                    AuditEntity.PRODUCT, id, updated.getName(), AuditDiff.between(before, snapshot(updated)))
                            .thenReturn(updated));
                }));
    }

    @Override
    public Mono<Void> delete(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(existing -> doDelete(id)
                        .then(Mono.defer(() -> audit.record(AuditAction.DELETE, AuditEntity.PRODUCT, id,
                                existing.getName(), AuditDiff.describe(snapshot(existing)))))));
    }

    private static Map<String, Object> snapshot(Product product) {
        Map<String, Object> snapshot = AuditDiff.snapshot();
        snapshot.put("Nombre", product.getName());
        snapshot.put("Descripción", product.getDescription());
        snapshot.put("SKU", product.getSku());
        snapshot.put("Código de barras", product.getBarcode());
        snapshot.put("Categoría", product.getCategoryName());
        snapshot.put("Unidad", product.getUnit());
        snapshot.put("Costo", product.getCostPrice());
        snapshot.put("Precio de venta", product.getSalePrice());
        snapshot.put("Stock mínimo", product.getMinStock());
        snapshot.put("Controla vencimiento", product.getTracksExpiry());
        snapshot.put("Activo", product.getIsActive());
        return snapshot;
    }

    private Mono<Product> doCreate(Product product) {
        return SecurityHelper.currentCompanyId().flatMap(companyId -> {
            normalize(product);
            product.setCompanyId(companyId);
            if (product.getIsActive() == null) {
                product.setIsActive(true);
            }
            if (product.getMinStock() == null) {
                product.setMinStock(BigDecimal.ZERO);
            }
            if (product.getTracksExpiry() == null) {
                product.setTracksExpiry(false);
            }
            return loadSettings(companyId)
                    .flatMap(settings -> validateRequiredFields(settings, product))
                    .then(validateCategory(companyId, product.getCategoryId()))
                    .then(validateUnique(companyId, product, null))
                    .then(Mono.defer(() -> productRepositoryPort.save(product)))
                    .flatMap(this::withCategoryName);
        });
    }

    private Mono<Product> doUpdate(Long id, Product changes) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId)
                        .flatMap(existing -> {
                            normalize(changes);
                            boolean changesUnit = changes.getUnit() != null && existing.getUnit() != changes.getUnit();
                            boolean stopsTrackingExpiry = Boolean.TRUE.equals(existing.getTracksExpiry())
                                    && Boolean.FALSE.equals(changes.getTracksExpiry());
                            existing.setCategoryId(changes.getCategoryId());
                            existing.setName(changes.getName());
                            existing.setDescription(changes.getDescription());
                            existing.setSku(changes.getSku());
                            existing.setBarcode(changes.getBarcode());
                            existing.setUnit(changes.getUnit());
                            existing.setCostPrice(changes.getCostPrice());
                            existing.setSalePrice(changes.getSalePrice());
                            if (changes.getMinStock() != null) {
                                existing.setMinStock(changes.getMinStock());
                            }
                            if (changes.getTracksExpiry() != null) {
                                existing.setTracksExpiry(changes.getTracksExpiry());
                            }
                            if (changes.getIsActive() != null) {
                                existing.setIsActive(changes.getIsActive());
                            }
                            existing.setUpdatedAt(LocalDateTime.now());
                            return loadSettings(companyId)
                                    .flatMap(settings -> validateRequiredFields(settings, existing))
                                    .then(validateCategory(companyId, existing.getCategoryId()))
                                    .then(validateUnique(companyId, existing, id))
                                    .then(validateCanStopTrackingExpiry(id, stopsTrackingExpiry))
                                    .then(validateCanChangeUnit(id, changesUnit))
                                    .then(Mono.defer(() -> productRepositoryPort.save(existing)));
                        })
                        .flatMap(this::withCategoryName));
    }

    /** A product with stock history can't be deleted (the log would lose its subject); it must be deactivated. */
    private Mono<Void> doDelete(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId)
                        .flatMap(existing -> movementRepositoryPort.existsByProductId(id))
                        .flatMap(hasHistory -> {
                            if (hasHistory) {
                                return Mono.<Void>error(new ConflictException(
                                        "El producto tiene movimientos de stock; desactívalo en lugar de eliminarlo"));
                            }
                            return productRepositoryPort.deleteById(id);
                        }));
    }

    @Override
    public Mono<Product> findById(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(this::withCategoryName));
    }

    @Override
    public Mono<PageResponse<Product>> findAll(int page, int size, String search, Long categoryId, Boolean isActive) {
        int safePage = SearchSupport.safePage(page);
        int safeSize = SearchSupport.safeSize(size);
        String pattern = SearchSupport.likePattern(search);

        return SecurityHelper.currentCompanyId().flatMap(companyId -> Mono.zip(
                        productRepositoryPort.findPage(companyId, categoryId, isActive, pattern, safePage, safeSize)
                                .collectList()
                                .flatMap(this::withCategoryNames),
                        productRepositoryPort.count(companyId, categoryId, isActive, pattern))
                .map(tuple -> PageResponse.of(tuple.getT1(), safePage, safeSize, tuple.getT2())));
    }

    /**
     * Without expiry control the product's stock no longer touches its lots, so any lot still holding stock would
     * stay on the Vencimientos screen forever and drift from the real stock. It has to be emptied first.
     */
    private Mono<Void> validateCanStopTrackingExpiry(Long productId, boolean stopsTracking) {
        if (!stopsTracking) {
            return Mono.empty();
        }
        return lotRepositoryPort.existsWithStock(productId)
                .filter(Boolean::booleanValue)
                .flatMap(hasLots -> Mono.<Void>error(new ConflictException(
                        "El producto aún tiene lotes con stock; sácalos o ajústalos a 0 antes de dejar de controlar el vencimiento")));
    }

    /**
     * The stock, the movements and the open orders of a product are written in its unit; switching from liters to
     * units (or any other pair) with units still in the warehouse would silently re-label them. It has to be at
     * zero first.
     */
    private Mono<Void> validateCanChangeUnit(Long productId, boolean changesUnit) {
        if (!changesUnit) {
            return Mono.empty();
        }
        return stockRepositoryPort.existsStock(productId)
                .filter(Boolean::booleanValue)
                .flatMap(hasStock -> Mono.<Void>error(new ConflictException(
                        "El producto tiene stock; no se puede cambiar su unidad de medida mientras tenga existencias")));
    }

    private Mono<Product> find(Long id, Long companyId) {
        return productRepositoryPort.findByIdAndCompanyId(id, companyId)
                .switchIfEmpty(Mono.error(NotFoundException.forResource(RESOURCE, id)));
    }

    private Mono<CompanySettings> loadSettings(Long companyId) {
        return settingsRepositoryPort.findByCompanyId(companyId)
                .defaultIfEmpty(CompanySettings.defaults(companyId));
    }

    /** Applies the company's "mandatory field" settings; the fields are optional unless the company says otherwise. */
    private Mono<Void> validateRequiredFields(CompanySettings settings, Product product) {
        if (Boolean.TRUE.equals(settings.getRequireProductSku()) && product.getSku() == null) {
            return Mono.error(new BadRequestException("El SKU es obligatorio según la configuración de tu empresa"));
        }
        if (Boolean.TRUE.equals(settings.getRequireProductCategory()) && product.getCategoryId() == null) {
            return Mono.error(new BadRequestException("La categoría es obligatoria según la configuración de tu empresa"));
        }
        if (Boolean.TRUE.equals(settings.getRequireProductBarcode()) && product.getBarcode() == null) {
            return Mono.error(new BadRequestException("El código de barras es obligatorio según la configuración de tu empresa"));
        }
        return Mono.empty();
    }

    /** A product may have no category, but if it names one it must belong to the caller's company. */
    private Mono<Void> validateCategory(Long companyId, Long categoryId) {
        if (categoryId == null) {
            return Mono.empty();
        }
        return categoryRepositoryPort.findByIdAndCompanyId(categoryId, companyId)
                .switchIfEmpty(Mono.error(NotFoundException.forResource("Product category", categoryId)))
                .then();
    }

    private Mono<Void> validateUnique(Long companyId, Product product, Long excludeId) {
        // Both are optional now; only the ones present need to be unique.
        Mono<Void> skuCheck = product.getSku() == null
                ? Mono.empty()
                : productRepositoryPort.existsBySku(companyId, product.getSku(), excludeId)
                        .filter(Boolean::booleanValue)
                        .flatMap(exists -> Mono.<Void>error(
                                new ConflictException("Ya existe un producto con el SKU '" + product.getSku() + "'")));

        Mono<Void> barcodeCheck = product.getBarcode() == null
                ? Mono.empty()
                : productRepositoryPort.existsByBarcode(companyId, product.getBarcode(), excludeId)
                        .filter(Boolean::booleanValue)
                        .flatMap(exists -> Mono.<Void>error(
                                new ConflictException("Ya existe un producto con el código de barras '" + product.getBarcode() + "'")));

        return skuCheck.then(barcodeCheck);
    }

    private Mono<Product> withCategoryName(Product product) {
        return withCategoryNames(List.of(product)).map(products -> products.get(0));
    }

    /** One category lookup for the whole page instead of one per product. */
    private Mono<List<Product>> withCategoryNames(List<Product> products) {
        Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (categoryIds.isEmpty()) {
            return Mono.just(products);
        }

        return categoryRepositoryPort.findAllByIds(categoryIds)
                .collectMap(ProductCategory::getId, ProductCategory::getName)
                .map(namesById -> {
                    attachCategoryNames(products, namesById);
                    return products;
                });
    }

    private void attachCategoryNames(List<Product> products, Map<Long, String> namesById) {
        products.forEach(product -> product.setCategoryName(
                product.getCategoryId() == null ? null : namesById.get(product.getCategoryId())));
    }

    private void normalize(Product product) {
        product.setName(product.getName() == null ? null : product.getName().trim());
        product.setSku(SearchSupport.blankToNull(product.getSku()));
        product.setBarcode(SearchSupport.blankToNull(product.getBarcode()));
        product.setDescription(SearchSupport.blankToNull(product.getDescription()));
    }
}
