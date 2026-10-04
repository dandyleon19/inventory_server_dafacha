package com.dafacha.inventory.domain.model;

public enum MovementType {
    ENTRY,
    /** Goods received against a purchase order. */
    PURCHASE,
    EXIT,
    ADJUSTMENT,
    TRANSFER_IN,
    TRANSFER_OUT
}
