package com.dafacha.inventory.domain.model;

public enum UnitOfMeasure {
    UNIT,
    KILOGRAM,
    GRAM,
    LITER,
    MILLILITER,
    METER,
    BOX,
    PACK,
    DOZEN;

    /** Units that can't be split: a product counted in these takes whole quantities only. */
    public boolean isWholeOnly() {
        return this == UNIT || this == BOX || this == PACK || this == DOZEN;
    }
}
