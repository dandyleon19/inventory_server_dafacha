package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.CostLayer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CostLayerRepositoryPort {

    /** A new layer: {@code quantity} units that cost {@code unitCost} each. A null expiry date means "no lot". */
    Mono<Void> add(Long companyId, Long warehouseId, Long productId, BigDecimal unitCost, BigDecimal quantity,
                   LocalDate expiryDate, String lotNumber);

    /**
     * The layers of one lot (or of the units without a lot, when {@code expiryDate} is null) that still have
     * something left, oldest first, locked so a concurrent exit can't take the same units. Inside the transaction.
     */
    Flux<CostLayer> lockOldestFirst(Long warehouseId, Long productId, LocalDate expiryDate, String lotNumber);

    /** Every layer of the product in the warehouse with something left, oldest first, locked. */
    Flux<CostLayer> lockAllOldestFirst(Long warehouseId, Long productId);

    /** Takes {@code quantity} from a layer; one that reaches zero is deleted. */
    Mono<Void> consume(Long layerId, BigDecimal quantity);

    /** For the exit form's "which purchase?" list: not locked, oldest first. */
    Flux<CostLayer> findAvailable(Long warehouseId, Long productId);
}
