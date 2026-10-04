package com.dafacha.inventory.domain.ports.in;

import com.dafacha.inventory.domain.model.Company;
import com.dafacha.inventory.domain.model.CompanySettings;
import reactor.core.publisher.Mono;

/** The authenticated user's own company: its profile and its settings. */
public interface CompanyUseCase {
    Mono<Company> getCompany();
    Mono<Company> updateCompany(Company changes);
    Mono<CompanySettings> getSettings();
    Mono<CompanySettings> updateSettings(CompanySettings changes);
}
