package com.dafacha.inventory.domain.model;

import java.math.BigDecimal;

/**
 * What the user counted in one lot during a physical count. A null {@code lotId} means the units without an
 * expiry date (stock that existed before the product started controlling expiry).
 */
public record LotCount(Long lotId, BigDecimal countedQuantity) {
}
