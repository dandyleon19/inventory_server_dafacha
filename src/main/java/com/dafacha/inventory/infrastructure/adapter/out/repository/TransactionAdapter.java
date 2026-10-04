package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.ports.out.TransactionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class TransactionAdapter implements TransactionPort {

    private final TransactionalOperator transactionalOperator;

    @Override
    public <T> Mono<T> execute(Mono<T> action) {
        return transactionalOperator.transactional(action);
    }
}
