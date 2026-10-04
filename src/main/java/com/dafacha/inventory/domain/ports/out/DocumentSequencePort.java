package com.dafacha.inventory.domain.ports.out;

import reactor.core.publisher.Mono;

/** Gap-free-per-company counters for document numbers. */
public interface DocumentSequencePort {
    /** Atomically increments and returns the next value for (company, sequence name). Starts at 1. */
    Mono<Long> next(Long companyId, String sequenceName);
}
