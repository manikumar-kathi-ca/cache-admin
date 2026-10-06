package com.financialcorp.cachepoc.service;

import com.financialcorp.cachepoc.entity.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheSyncService {

    public static final String ACCOUNTS_CACHE = "accounts";

    private static final Logger log = LoggerFactory.getLogger(CacheSyncService.class);

    private final CacheManager cacheManager;

    public CacheSyncService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void putAccount(Account account) {
        if (account == null || account.getId() == null) {
            return;
        }
        Cache cache = cacheManager.getCache(ACCOUNTS_CACHE);
        if (cache != null) {
            cache.put(account.getId(), account);
            log.debug("Wrote account {} to Redis cache", account.getId());
        }
    }

    public void evictAccount(Long id) {
        if (id == null) {
            return;
        }
        Cache cache = cacheManager.getCache(ACCOUNTS_CACHE);
        if (cache != null) {
            cache.evict(id);
            log.debug("Evicted account {} from Redis cache", id);
        }
    }
}
