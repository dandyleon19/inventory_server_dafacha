package com.dafacha.inventory.application.service;

/** Who is operating, and the company rule that decides whether stock may go below zero. */
public record StockContext(Long companyId, Long userId, boolean allowNegative) {
}
