package com.financialcorp.cachepoc.controller;

import com.financialcorp.cachepoc.service.CacheAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/caches")
@Tag(name = "Cache admin", description = "Inspect, flush, or drop Redis cache")
public class CacheAdminController {

    private final CacheAdminService cacheAdminService;

    public CacheAdminController(CacheAdminService cacheAdminService) {
        this.cacheAdminService = cacheAdminService;
    }

    @GetMapping
    @Operation(summary = "List cache regions and keys")
    public List<Map<String, Object>> list() {
        return cacheAdminService.listCaches();
    }

    @GetMapping("/status")
    @Operation(summary = "Cache backend status")
    public Map<String, Object> status() {
        return cacheAdminService.status();
    }

    @PostMapping("/{cacheName}/flush")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Flush one cache region")
    public void flush(@PathVariable String cacheName) {
        cacheAdminService.flushCache(cacheName);
    }

    @PostMapping("/flush-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Flush all Spring cache regions")
    public void flushAll() {
        cacheAdminService.flushAllCaches();
    }

    @PostMapping("/flush-redis")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Drop entire Redis DB (FLUSHDB)")
    public void flushRedis() {
        cacheAdminService.flushRedisDatabase();
    }

    @DeleteMapping("/{cacheName}/keys/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Evict a single cache key")
    public void evictKey(@PathVariable String cacheName, @PathVariable String key) {
        cacheAdminService.evictKey(cacheName, key);
    }
}
