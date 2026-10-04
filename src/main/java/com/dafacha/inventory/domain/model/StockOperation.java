package com.dafacha.inventory.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Input for a stock operation. Which fields apply depends on the operation (see StockUseCase). */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockOperation {
    private Long warehouseId;
    /** Only for transfers: the destination (warehouseId is the origin). */
    private Long toWarehouseId;
    private Long productId;
    /** Entries, exits and transfers: how much moves (always positive). */
    private BigDecimal quantity;
    /** Adjustments only: the physically counted quantity, which becomes the new stock. */
    private BigDecimal countedQuantity;
    /** Entries and upward adjustments of an expiry-tracked product: the lot that is being added. */
    private LocalDate expiryDate;
    private String lotNumber;
    /**
     * Exits and transfers of an expiry-tracked product: which lots the quantity comes from. Null or empty means
     * "the ones that expire first" (FEFO). When present, the picks must add up to {@code quantity}.
     */
    private List<LotPick> lots;
    /** Entries: what each unit cost. Null means the product's current cost. */
    private BigDecimal unitCost;
    /**
     * Exits and transfers of a product WITHOUT expiry control, when the company chooses the purchase an exit
     * comes from (cost method MANUAL). Null or empty means the oldest purchase first.
     */
    private List<LayerPick> layerPicks;
    /**
     * Adjustments of an expiry-tracked product, counted lot by lot: what was counted in each lot the system
     * knows. When present, {@code countedQuantity} is the total of these plus {@code newLots}.
     */
    private List<LotCount> lotCounts;
    /** Lots found in the count that the system did not have. */
    private List<NewLot> newLots;
    private String notes;
}
