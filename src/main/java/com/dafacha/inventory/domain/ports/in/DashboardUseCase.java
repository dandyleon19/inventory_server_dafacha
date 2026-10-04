package com.dafacha.inventory.domain.ports.in;

import com.dafacha.inventory.domain.model.DashboardSummary;
import reactor.core.publisher.Mono;

public interface DashboardUseCase {
    /** Valuation, alert counters (stock, expiry, purchase arrivals) and short lists for the caller's company. */
    Mono<DashboardSummary> getSummary();
}
