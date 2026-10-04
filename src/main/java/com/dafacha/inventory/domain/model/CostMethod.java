package com.dafacha.inventory.domain.model;

/** How the cost of what leaves the warehouse is decided. */
public enum CostMethod {
    /** First in, first out: the oldest purchase leaves first. Nobody has to choose. */
    FIFO,
    /** The user picks which purchase an exit (or transfer) comes from. */
    MANUAL
}
