package com.dafacha.inventory.domain.ports.out;

/**
 * A user's role, status and warehouses are cached for a few seconds per request; whoever changes them calls
 * this so the change applies at once instead of when the cache entry expires.
 */
public interface UserAccessInvalidator {
    void evict(Long userId);
}
