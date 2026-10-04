package com.dafacha.inventory.domain.ports.out;

import reactor.core.publisher.Mono;

/** Runs a reactive flow inside one database transaction: it all commits, or (on error) none of it does. */
public interface TransactionPort {
    <T> Mono<T> execute(Mono<T> action);
}
