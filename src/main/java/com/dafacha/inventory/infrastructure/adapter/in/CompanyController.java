package com.dafacha.inventory.infrastructure.adapter.in;

import com.dafacha.inventory.application.dto.request.UpdateCompanyRequest;
import com.dafacha.inventory.application.dto.request.UpdateCompanySettingsRequest;
import com.dafacha.inventory.application.dto.response.CompanyResponse;
import com.dafacha.inventory.application.dto.response.CompanySettingsResponse;
import com.dafacha.inventory.application.mapper.RequestDtoMapper;
import com.dafacha.inventory.domain.ports.in.CompanyUseCase;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.CompanyMapper;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.CompanySettingsMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** The authenticated user's own company. There is no company id in the path on purpose. */
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyUseCase companyUseCase;
    private final RequestDtoMapper requestDtoMapper;
    private final CompanyMapper companyMapper;
    private final CompanySettingsMapper companySettingsMapper;

    @GetMapping
    public Mono<ResponseEntity<CompanyResponse>> getCompany() {
        return companyUseCase.getCompany()
                .map(companyMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PutMapping
    public Mono<ResponseEntity<CompanyResponse>> updateCompany(@Valid @RequestBody UpdateCompanyRequest request) {
        return companyUseCase.updateCompany(requestDtoMapper.toCompany(request))
                .map(companyMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/settings")
    public Mono<ResponseEntity<CompanySettingsResponse>> getSettings() {
        return companyUseCase.getSettings()
                .map(companySettingsMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/settings")
    public Mono<ResponseEntity<CompanySettingsResponse>> updateSettings(
            @Valid @RequestBody UpdateCompanySettingsRequest request
    ) {
        return companyUseCase.updateSettings(requestDtoMapper.toCompanySettings(request))
                .map(companySettingsMapper::toResponse)
                .map(ResponseEntity::ok);
    }
}
