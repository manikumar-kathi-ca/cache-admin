package com.financialcorp.cachepoc.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CacheAdminService {

    private final CacheManager cacheManager;
    private final RedisConnectionFactory redisConnectionFactory;

    public CacheAdminService(
            CacheManager cacheManager, ObjectProvider<RedisConnectionFactory> redisConnectionFactory) {
        this.cacheManager = cacheManager;
        this.redisConnectionFactory = redisConnectionFactory.getIfAvailable();
    }

    public List<Map<String, Object>> listCaches() {
        Collection<String> names = cacheManager.getCacheNames();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : names) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            row.put("keys", listKeys(name));
            result.add(row);
        }
        return result;
    }

    public List<String> listKeys(String cacheName) {
        requireCache(cacheName);
        if (redisConnectionFactory != null) {
            return scanRedisKeys(cacheName + "::*");
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof ConcurrentMapCache concurrentMapCache) {
            return concurrentMapCache.getNativeCache().keySet().stream()
                    .map(String::valueOf)
                    .sorted()
                    .toList();
        }
        return List.of();
    }

    public void flushCache(String cacheName) {
        requireCache(cacheName).clear();
    }

    public void flushAllCaches() {
        cacheManager.getCacheNames().forEach(this::flushCache);
    }

    public void evictKey(String cacheName, String key) {
        Cache cache = requireCache(cacheName);
        cache.evict(coerceKey(key));
        cache.evict(key);
    }

    public void flushRedisDatabase() {
        if (redisConnectionFactory == null) {
            flushAllCaches();
            return;
        }
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("backend", redisConnectionFactory != null ? "redis" : "simple");
        status.put("caches", cacheManager.getCacheNames());
        status.put("keyCount", cacheManager.getCacheNames().stream()
                .mapToInt(name -> listKeys(name).size())
                .sum());
        return status;
    }

    private Cache requireCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cache not found: " + cacheName);
        }
        return cache;
    }

    private List<String> scanRedisKeys(String pattern) {
        TreeSet<String> keys = new TreeSet<>();
        try (RedisConnection connection = redisConnectionFactory.getConnection();
                Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        }
        return new ArrayList<>(keys);
    }

    private Object coerceKey(String key) {
        if (key == null) {
            return null;
        }
        try {
            return Long.valueOf(key);
        } catch (NumberFormatException ex) {
            return key;
        }
    }
}
