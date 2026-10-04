package com.dafacha.inventory.application.service;

import com.dafacha.inventory.application.security.SecurityHelper;
import com.dafacha.inventory.application.support.SearchSupport;
import com.dafacha.inventory.application.support.AuditDiff;
import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.exception.ConflictException;
import com.dafacha.inventory.domain.exception.NotFoundException;
import com.dafacha.inventory.domain.model.Company;
import com.dafacha.inventory.domain.model.CompanySettings;
import com.dafacha.inventory.domain.ports.in.CompanyUseCase;
import com.dafacha.inventory.domain.ports.out.CompanyRepositoryPort;
import com.dafacha.inventory.domain.ports.out.CompanySettingsRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyUseCase {

    private final CompanyRepositoryPort companyRepositoryPort;
    private final CompanySettingsRepositoryPort settingsRepositoryPort;
    private final AuditRecorder audit;

    @Override
    public Mono<Company> getCompany() {
        return SecurityHelper.currentCompanyId().flatMap(this::find);
    }

    @Override
    public Mono<Company> updateCompany(Company changes) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(companyId).flatMap(existing -> {
                    Map<String, Object> before = snapshot(existing);
                    return doUpdateCompany(changes).flatMap(updated -> audit.record(AuditAction.UPDATE,
                                    AuditEntity.COMPANY, companyId, updated.getName(),
                                    AuditDiff.between(before, snapshot(updated)))
                            .thenReturn(updated));
                }));
    }

    private static Map<String, Object> snapshot(Company company) {
        Map<String, Object> snapshot = AuditDiff.snapshot();
        snapshot.put("Nombre", company.getName());
        snapshot.put("Razón social", company.getSocialReason());
        snapshot.put("Dirección fiscal", company.getFiscalAddress());
        snapshot.put("RUC", company.getRucNumber());
        snapshot.put("Teléfono", company.getPhone());
        return snapshot;
    }

    private static Map<String, Object> snapshot(CompanySettings settings) {
        Map<String, Object> snapshot = AuditDiff.snapshot();
        snapshot.put("SKU obligatorio", settings.getRequireProductSku());
        snapshot.put("Categoría obligatoria", settings.getRequireProductCategory());
        snapshot.put("Código de barras obligatorio", settings.getRequireProductBarcode());
        snapshot.put("Permitir stock negativo", settings.getAllowNegativeStock());
        snapshot.put("Días de alerta de vencimiento", settings.getExpiryAlertDays());
        snapshot.put("Días de alerta de llegada de compras", settings.getPurchaseArrivalAlertDays());
        snapshot.put("Método de costo", settings.getCostMethod() == null ? null
                : settings.getCostMethod() == com.dafacha.inventory.domain.model.CostMethod.FIFO
                        ? "Automático" : "Lo elijo yo en cada salida");
        return snapshot;
    }

    private Mono<Company> doUpdateCompany(Company changes) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(companyId).flatMap(existing -> {
                    existing.setName(changes.getName() == null ? null : changes.getName().trim());
                    existing.setSocialReason(SearchSupport.blankToNull(changes.getSocialReason()));
                    existing.setFiscalAddress(SearchSupport.blankToNull(changes.getFiscalAddress()));
                    existing.setRucNumber(SearchSupport.blankToNull(changes.getRucNumber()));
                    existing.setPhone(SearchSupport.blankToNull(changes.getPhone()));
                    existing.setUpdatedAt(LocalDateTime.now());
                    return validateUniqueRuc(existing)
                            .then(Mono.defer(() -> companyRepositoryPort.save(existing)));
                }));
    }

    @Override
    public Mono<CompanySettings> getSettings() {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                settingsRepositoryPort.findByCompanyId(companyId)
                        .defaultIfEmpty(CompanySettings.defaults(companyId)));
    }

    @Override
    public Mono<CompanySettings> updateSettings(CompanySettings changes) {
        return getSettings().flatMap(existing -> {
            Map<String, Object> before = snapshot(existing);
            return doUpdateSettings(changes).flatMap(updated -> audit.record(AuditAction.UPDATE,
                            AuditEntity.SETTINGS, updated.getCompanyId(), "Configuración",
                            AuditDiff.between(before, snapshot(updated)))
                    .thenReturn(updated));
        });
    }

    private Mono<CompanySettings> doUpdateSettings(CompanySettings changes) {
        return SecurityHelper.currentCompanyId().flatMap(companyId -> {
            changes.setCompanyId(companyId);
            return settingsRepositoryPort.upsert(changes);
        });
    }

    private Mono<Company> find(Long companyId) {
        return companyRepositoryPort.findById(companyId)
                .switchIfEmpty(Mono.error(NotFoundException.forResource("Company", companyId)));
    }

    private Mono<Void> validateUniqueRuc(Company company) {
        if (company.getRucNumber() == null) {
            return Mono.empty();
        }
        return companyRepositoryPort.existsByRucNumber(company.getRucNumber(), company.getId())
                .filter(Boolean::booleanValue)
                .flatMap(exists -> Mono.<Void>error(
                        new ConflictException("Ya existe una empresa con el RUC '" + company.getRucNumber() + "'")));
    }
}
