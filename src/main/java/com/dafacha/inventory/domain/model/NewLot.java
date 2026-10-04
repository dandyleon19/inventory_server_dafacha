package com.dafacha.inventory.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A lot found during a physical count that the system did not have: it is created with this quantity. */
public record NewLot(LocalDate expiryDate, String lotNumber, BigDecimal quantity) {
}
