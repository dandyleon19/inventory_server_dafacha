package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.CompanySettings;
import reactor.core.publisher.Mono;

public interface CompanySettingsRepositoryPort {
    /** Empty when the company has never saved settings (callers fall back to the defaults). */
    Mono<CompanySettings> findByCompanyId(Long companyId);
    /** Insert-or-update, so the first save and later saves behave the same. */
    Mono<CompanySettings> upsert(CompanySettings settings);
}
