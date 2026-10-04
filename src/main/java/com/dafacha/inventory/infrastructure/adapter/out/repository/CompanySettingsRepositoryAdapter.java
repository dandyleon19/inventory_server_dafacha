package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.CompanySettings;
import com.dafacha.inventory.domain.model.CostMethod;
import com.dafacha.inventory.domain.ports.out.CompanySettingsRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.CompanySettingsMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CompanySettingsRepositoryAdapter implements CompanySettingsRepositoryPort {

    private final CompanySettingsRepository repository;
    private final CompanySettingsMapper mapper;

    @Override
    public Mono<CompanySettings> findByCompanyId(Long companyId) {
        return repository.findById(companyId).map(mapper::toDomain);
    }

    @Override
    public Mono<CompanySettings> upsert(CompanySettings settings) {
        return repository.upsert(
                        settings.getCompanyId(),
                        Boolean.TRUE.equals(settings.getRequireProductSku()),
                        Boolean.TRUE.equals(settings.getRequireProductCategory()),
                        Boolean.TRUE.equals(settings.getRequireProductBarcode()),
                        Boolean.TRUE.equals(settings.getAllowNegativeStock()),
                        settings.getExpiryAlertDays() == null ? 30 : settings.getExpiryAlertDays(),
                        settings.getPurchaseArrivalAlertDays() == null ? 7 : settings.getPurchaseArrivalAlertDays(),
                        settings.getCostMethod() == null ? CostMethod.FIFO.name() : settings.getCostMethod().name())
                .then(Mono.defer(() -> findByCompanyId(settings.getCompanyId())));
    }
}
