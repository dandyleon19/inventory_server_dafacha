package com.dafacha.inventory.domain.model;

/** Which lots a listing should show. "Expiring" uses the company's alert window. */
public enum LotScope {
    /** Every lot with stock. */
    ALL,
    /** Already past their expiry date. */
    EXPIRED,
    /** Not expired yet, but within the alert window. */
    EXPIRING,
    /** Expired or expiring: everything that needs attention. */
    ATTENTION
}
