package com.dafacha.inventory.domain.model;

import java.math.BigDecimal;

/**
 * How much to take from one lot, chosen by the user. A null {@code lotId} means the units of that product that
 * have no expiry date (stock that existed before the product started controlling expiry).
 */
public record LotPick(Long lotId, BigDecimal quantity) {
}
