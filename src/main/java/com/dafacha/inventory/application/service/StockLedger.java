package com.dafacha.inventory.application.service;

import com.dafacha.inventory.domain.exception.BadRequestException;
import com.dafacha.inventory.domain.exception.ConflictException;
import com.dafacha.inventory.domain.model.CostLayer;
import com.dafacha.inventory.domain.model.LayerPick;
import com.dafacha.inventory.domain.model.LotPick;
import com.dafacha.inventory.domain.model.MovementType;
import com.dafacha.inventory.domain.model.Product;
import com.dafacha.inventory.domain.model.StockLot;
import com.dafacha.inventory.domain.model.StockMovement;
import com.dafacha.inventory.domain.model.Warehouse;
import com.dafacha.inventory.domain.ports.out.CostLayerRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockLotRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockMovementRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The one place where stock actually changes: it applies the quantity, keeps the lots of expiry-tracked
 * products, and writes the movement log. Entries, exits, transfers, adjustments and purchase receipts all go
 * through it, so lot handling (first-expired-first-out) can't diverge between them.
 *
 * Must be called inside the operation's transaction.
 */
@RequiredArgsConstructor
public class StockLedger {

    /**
     * A quantity of a lot. A null expiry date means "no lot" (stock that was never given an expiry). For what leaves
     * the warehouse, {@code unitCost} is the real cost of those units (from the cost layer they came from); null
     * where it isn't known or doesn't apply.
     */
    public record LotSlice(String lotNumber, LocalDate expiryDate, BigDecimal quantity, BigDecimal unitCost) {
        public LotSlice(String lotNumber, LocalDate expiryDate, BigDecimal quantity) {
            this(lotNumber, expiryDate, quantity, null);
        }
    }

    /** What an operation did: the movements it logged and which lots the quantity came from / went to. */
    public record Result(List<StockMovement> movements, List<LotSlice> slices) {
    }

    /**
     * What is being moved and why. {@code unitCost} null means "the product's current cost"; {@code occurredAt}
     * null means "now" (a receipt registered a day late carries the day the goods really arrived).
     */
    public record Change(MovementType type, BigDecimal quantity, String notes, String transferRef,
                         Long purchaseOrderId, BigDecimal unitCost, LocalDateTime occurredAt) {
    }

    private final StockRepositoryPort stockRepositoryPort;
    private final StockLotRepositoryPort lotRepositoryPort;
    private final StockMovementRepositoryPort movementRepositoryPort;
    private final CostLayerRepositoryPort costLayerRepositoryPort;

    /**
     * Adds {@code change.quantity()} to the stock. For an expiry-tracked product the lot is added to its lot
     * list; when {@code lotRequired} (a user entering stock, as opposed to a transfer moving existing lots) the
     * expiry date must be present and not in the past.
     */
    public Mono<Result> credit(StockContext ctx, Warehouse warehouse, Product product, Change change,
                               LotSlice lot, boolean lotRequired) {
        boolean tracked = Boolean.TRUE.equals(product.getTracksExpiry());
        BigDecimal quantity = change.quantity();

        LotSlice usedLot = null;
        if (tracked) {
            boolean hasExpiry = lot != null && lot.expiryDate() != null;
            if (lotRequired && !hasExpiry) {
                return Mono.error(new BadRequestException(
                        "'" + product.getName() + "' controla vencimiento: indica la fecha de vencimiento"));
            }
            if (lotRequired && lot.expiryDate().isBefore(LocalDate.now())) {
                return Mono.error(new BadRequestException(
                        "La fecha de vencimiento de '" + product.getName() + "' ya pasó"));
            }
            if (hasExpiry) {
                usedLot = new LotSlice(lot.lotNumber() == null ? "" : lot.lotNumber().trim(), lot.expiryDate(), quantity);
            }
        }

        LotSlice finalLot = usedLot;
        BigDecimal unitCost = change.unitCost() != null ? change.unitCost() : product.getCostPrice();
        return stockRepositoryPort
                .applyDelta(ctx.companyId(), warehouse.getId(), product.getId(), quantity, true)
                .flatMap(balance -> {
                    Mono<Void> lotWrite = finalLot == null
                            ? Mono.empty()
                            : lotRepositoryPort.addToLot(ctx.companyId(), warehouse.getId(), product.getId(),
                                    finalLot.lotNumber(), finalLot.expiryDate(), quantity);

                    // If the stock was negative (allowed by the company), the first units that arrive only cover
                    // the units already given out: they have no layer, because they are not in the warehouse.
                    BigDecimal deficit = balance.subtract(quantity).negate().max(BigDecimal.ZERO);
                    BigDecimal layerQuantity = quantity.subtract(quantity.min(deficit));
                    Mono<Void> layerWrite = layerQuantity.signum() > 0
                            ? costLayerRepositoryPort.add(ctx.companyId(), warehouse.getId(), product.getId(), unitCost,
                                    layerQuantity, finalLot == null ? null : finalLot.expiryDate(),
                                    finalLot == null ? "" : finalLot.lotNumber())
                            : Mono.empty();

                    return lotWrite.then(layerWrite).then(Mono.defer(() -> movementRepositoryPort.save(
                            movement(ctx, warehouse, product, change, quantity, balance, finalLot, unitCost))));
                })
                .map(saved -> new Result(List.of(saved), finalLot == null ? List.of() : List.of(finalLot)));
    }

    /**
     * Takes {@code change.quantity()} out of the stock. For an expiry-tracked product it comes from the lots that
     * expire first (FEFO); anything beyond the lots (stock that predates expiry tracking) comes last, without a
     * lot. Rejected when it would leave the stock negative, unless the company allows that.
     */
    public Mono<Result> debit(StockContext ctx, Warehouse warehouse, Product product, Change change) {
        return debit(ctx, warehouse, product, change, null, null);
    }

    /**
     * Like {@link #debit(StockContext, Warehouse, Product, Change)}, but for an expiry-tracked product the caller
     * may say exactly which lots the quantity comes from ({@code picks}, which must add up to the quantity). With
     * no picks it falls back to FEFO.
     */
    public Mono<Result> debit(StockContext ctx, Warehouse warehouse, Product product, Change change,
                              List<LotPick> picks) {
        return debit(ctx, warehouse, product, change, picks, null);
    }

    /**
     * The full form. On top of the lots, each unit that leaves takes the cost of the purchase (cost layer) it comes
     * from: the oldest layer of its lot first, or, for a product without expiry control, the layers in
     * {@code layerPicks} when the user chose them (company cost method MANUAL).
     */
    public Mono<Result> debit(StockContext ctx, Warehouse warehouse, Product product, Change change,
                              List<LotPick> picks, List<LayerPick> layerPicks) {
        BigDecimal quantity = change.quantity();
        boolean tracked = Boolean.TRUE.equals(product.getTracksExpiry());

        return stockRepositoryPort
                .applyDelta(ctx.companyId(), warehouse.getId(), product.getId(), quantity.negate(), ctx.allowNegative())
                .switchIfEmpty(Mono.error(new ConflictException(
                        "Stock insuficiente de '" + product.getName() + "' en el almacén '" + warehouse.getName() + "'")))
                .flatMap(balanceAfter -> {
                    Mono<List<LotSlice>> allocation;
                    if (!tracked) {
                        allocation = Mono.just(List.of(new LotSlice(null, null, quantity)));
                    } else if (picks == null || picks.isEmpty()) {
                        allocation = allocateFefo(warehouse, product, quantity);
                    } else {
                        allocation = allocateChosen(warehouse, product, picks, quantity, balanceAfter, ctx.allowNegative());
                    }
                    return allocation
                            .flatMap(slices -> costSlices(warehouse, product, slices, tracked ? null : layerPicks))
                            .flatMap(costed -> saveOut(ctx, warehouse, product, change, costed, balanceAfter));
                });
    }

    // ---------------------------------------------------------------- internals

    /**
     * Gives every slice that leaves the real cost of its units, taking them out of the cost layers. A slice is
     * split in as many pieces as the layers it touches (each piece with its own cost). Units no layer covers
     * (stock that went negative) are costed at the product's current cost and consume nothing.
     */
    private Mono<List<LotSlice>> costSlices(Warehouse warehouse, Product product, List<LotSlice> slices,
                                            List<LayerPick> layerPicks) {
        return Flux.fromIterable(slices)
                .concatMap(slice -> layerPicks != null && !layerPicks.isEmpty()
                        ? costByPicks(warehouse, product, slice, layerPicks)
                        : costOldestFirst(warehouse, product, slice))
                .collectList()
                .map(lists -> lists.stream().flatMap(List::stream).toList());
    }

    private Mono<List<LotSlice>> costOldestFirst(Warehouse warehouse, Product product, LotSlice slice) {
        String lotNumber = slice.lotNumber() == null ? "" : slice.lotNumber();
        return costLayerRepositoryPort.lockOldestFirst(warehouse.getId(), product.getId(), slice.expiryDate(), lotNumber)
                .collectList()
                .flatMap(layers -> {
                    List<LotSlice> pieces = new ArrayList<>();
                    List<Mono<Void>> consumption = new ArrayList<>();
                    BigDecimal remaining = slice.quantity();

                    for (CostLayer layer : layers) {
                        if (remaining.signum() <= 0) {
                            break;
                        }
                        BigDecimal take = remaining.min(layer.getQuantity());
                        pieces.add(new LotSlice(slice.lotNumber(), slice.expiryDate(), take, layer.getUnitCost()));
                        consumption.add(costLayerRepositoryPort.consume(layer.getId(), take));
                        remaining = remaining.subtract(take);
                    }
                    if (remaining.signum() > 0) {
                        pieces.add(new LotSlice(slice.lotNumber(), slice.expiryDate(), remaining, product.getCostPrice()));
                    }
                    return Flux.concat(consumption).then(Mono.just(pieces));
                });
    }

    /** The user chose the purchases: each pick is taken from its layer; what the picks don't cover is costed as above. */
    private Mono<List<LotSlice>> costByPicks(Warehouse warehouse, Product product, LotSlice slice,
                                             List<LayerPick> layerPicks) {
        return costLayerRepositoryPort.lockAllOldestFirst(warehouse.getId(), product.getId())
                .collectList()
                .flatMap(layers -> {
                    Map<Long, CostLayer> byId = layers.stream()
                            .collect(Collectors.toMap(CostLayer::getId, Function.identity()));
                    List<LotSlice> pieces = new ArrayList<>();
                    List<Mono<Void>> consumption = new ArrayList<>();
                    BigDecimal remaining = slice.quantity();

                    for (LayerPick pick : layerPicks) {
                        CostLayer layer = byId.get(pick.layerId());
                        if (layer == null) {
                            return Mono.<List<LotSlice>>error(new BadRequestException(
                                    "La compra elegida ya no existe o no tiene stock en este almacén; vuelve a elegir"));
                        }
                        if (pick.quantity().compareTo(layer.getQuantity()) > 0) {
                            return Mono.<List<LotSlice>>error(new ConflictException(
                                    "La compra elegida solo tiene " + layer.getQuantity().stripTrailingZeros().toPlainString()
                                            + " disponible(s)"));
                        }
                        if (pick.quantity().compareTo(remaining) > 0) {
                            return Mono.<List<LotSlice>>error(new BadRequestException(
                                    "Las compras elegidas superan la cantidad que sale"));
                        }
                        pieces.add(new LotSlice(slice.lotNumber(), slice.expiryDate(), pick.quantity(), layer.getUnitCost()));
                        consumption.add(costLayerRepositoryPort.consume(layer.getId(), pick.quantity()));
                        remaining = remaining.subtract(pick.quantity());
                    }
                    if (remaining.signum() > 0) {
                        // Not covered by the picks (only possible with negative stock allowed): current cost, no layer.
                        pieces.add(new LotSlice(slice.lotNumber(), slice.expiryDate(), remaining, product.getCostPrice()));
                    }
                    return Flux.concat(consumption).then(Mono.just(pieces));
                });
    }

    /**
     * Takes from the lots the user chose. The lots are read locked, each pick must exist in this warehouse and have
     * enough, and the units without an expiry date can't exceed what the warehouse really has without one.
     */
    private Mono<List<LotSlice>> allocateChosen(Warehouse warehouse, Product product, List<LotPick> picks,
                                                BigDecimal quantity, BigDecimal balanceAfter, boolean allowNegative) {
        return lotRepositoryPort.lockFefo(warehouse.getId(), product.getId())
                .collectList()
                .flatMap(lots -> {
                    Map<Long, StockLot> byId = lots.stream()
                            .collect(Collectors.toMap(StockLot::getId, Function.identity()));
                    List<LotSlice> slices = new ArrayList<>();
                    List<StockLot> sources = new ArrayList<>();
                    BigDecimal withoutDate = BigDecimal.ZERO;

                    for (LotPick pick : picks) {
                        if (pick.lotId() == null) {
                            withoutDate = withoutDate.add(pick.quantity());
                            continue;
                        }
                        StockLot lot = byId.get(pick.lotId());
                        if (lot == null) {
                            return Mono.<List<LotSlice>>error(new BadRequestException(
                                    "El lote elegido ya no existe o no tiene stock en este almacén; vuelve a elegir"));
                        }
                        if (pick.quantity().compareTo(lot.getQuantity()) > 0) {
                            return Mono.<List<LotSlice>>error(new ConflictException(
                                    "El lote que vence el " + lot.getExpiryDate() + " solo tiene "
                                            + lot.getQuantity().stripTrailingZeros().toPlainString() + " disponible(s)"));
                        }
                        slices.add(new LotSlice(lot.getLotNumber(), lot.getExpiryDate(), pick.quantity()));
                        sources.add(lot);
                    }

                    if (withoutDate.signum() > 0) {
                        BigDecimal inLots = lots.stream().map(StockLot::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                        // Stock before this debit, minus what the lots account for = units that never got a date.
                        BigDecimal available = balanceAfter.add(quantity).subtract(inLots);
                        if (!allowNegative && withoutDate.compareTo(available) > 0) {
                            return Mono.<List<LotSlice>>error(new ConflictException(
                                    "Solo hay " + available.stripTrailingZeros().toPlainString()
                                            + " sin fecha de vencimiento de '" + product.getName() + "'"));
                        }
                        slices.add(new LotSlice(null, null, withoutDate)); // after the lot slices: sources line up with them
                    }

                    return Flux.range(0, sources.size())
                            .concatMap(i -> lotRepositoryPort.consume(sources.get(i).getId(), slices.get(i).quantity()))
                            .then(Mono.just(slices));
                });
    }

    /** Reads the lots (locked), splits the quantity over them earliest expiry first, and takes it from them. */
    private Mono<List<LotSlice>> allocateFefo(Warehouse warehouse, Product product, BigDecimal quantity) {
        return lotRepositoryPort.lockFefo(warehouse.getId(), product.getId())
                .collectList()
                .flatMap(lots -> {
                    List<LotSlice> slices = new ArrayList<>();
                    List<StockLot> sources = new ArrayList<>();
                    BigDecimal remaining = quantity;

                    for (StockLot lot : lots) {
                        if (remaining.signum() <= 0) {
                            break;
                        }
                        BigDecimal take = remaining.min(lot.getQuantity());
                        slices.add(new LotSlice(lot.getLotNumber(), lot.getExpiryDate(), take));
                        sources.add(lot);
                        remaining = remaining.subtract(take);
                    }
                    if (remaining.signum() > 0) {
                        slices.add(new LotSlice(null, null, remaining));
                    }

                    return Flux.range(0, sources.size())
                            .concatMap(i -> lotRepositoryPort.consume(sources.get(i).getId(), slices.get(i).quantity()))
                            .then(Mono.just((List<LotSlice>) slices));
                });
    }

    /**
     * One movement per slice. {@code balanceAfter} is the stock after the whole debit, so walking back from it
     * gives each row the stock right after that row.
     */
    private Mono<Result> saveOut(StockContext ctx, Warehouse warehouse, Product product, Change change,
                                 List<LotSlice> slices, BigDecimal balanceAfter) {
        BigDecimal running = balanceAfter.add(change.quantity());
        List<StockMovement> toSave = new ArrayList<>();

        for (LotSlice slice : slices) {
            running = running.subtract(slice.quantity());
            LotSlice shown = slice.expiryDate() == null ? null : slice;
            BigDecimal cost = slice.unitCost() != null
                    ? slice.unitCost()
                    : (change.unitCost() != null ? change.unitCost() : product.getCostPrice());
            toSave.add(movement(ctx, warehouse, product, change, slice.quantity().negate(), running, shown, cost));
        }

        return Flux.fromIterable(toSave)
                .concatMap(movementRepositoryPort::save)
                .collectList()
                .map(saved -> new Result(saved, slices));
    }

    private StockMovement movement(StockContext ctx, Warehouse warehouse, Product product, Change change,
                                   BigDecimal signedQuantity, BigDecimal balanceAfter, LotSlice lot,
                                   BigDecimal unitCost) {
        return StockMovement.builder()
                .companyId(ctx.companyId())
                .warehouseId(warehouse.getId())
                .productId(product.getId())
                .type(change.type())
                .quantity(signedQuantity)
                .balanceAfter(balanceAfter)
                .unitCost(unitCost)
                .notes(blankToNull(change.notes()))
                .transferRef(change.transferRef())
                .purchaseOrderId(change.purchaseOrderId())
                .lotNumber(lot == null || lot.lotNumber() == null || lot.lotNumber().isEmpty() ? null : lot.lotNumber())
                .expiryDate(lot == null ? null : lot.expiryDate())
                .userId(ctx.userId())
                .createdAt(change.occurredAt() != null ? change.occurredAt() : LocalDateTime.now())
                .build();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
