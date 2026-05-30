package com.travel.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeHotelCacheService {

    private static final String LOCAL_CACHE_KEY = "home";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${travel.hotel.home-cache.redis-key}")
    private String redisKey;

    @Value("${travel.hotel.home-cache.redis-ttl}")
    private Duration redisTtl;

    @Value("${travel.hotel.home-cache.local-ttl}")
    private Duration localTtl;

    @Value("${travel.hotel.home-cache.local-max-size}")
    private long localMaxSize;

    @Value("${travel.hotel.home-cache.nginx-purge-enabled:true}")
    private boolean nginxPurgeEnabled;

    @Value("${travel.hotel.home-cache.nginx-cache-path:/var/cache/nginx/travel}")
    private Path nginxCachePath;

    private Cache<String, HotelSearchService.HotelSearchResult> localCache;

    @PostConstruct
    void initLocalCache() {
        localCache = Caffeine.newBuilder()
                .maximumSize(localMaxSize)
                .expireAfterWrite(localTtl)
                .build();
    }


    public boolean isHomeAllRequest(String keyword,
                                    String city,
                                    String starName,
                                    String brand,
                                    String priceRange,
                                    Long current,
                                    Boolean all) {
        return Boolean.TRUE.equals(all)
                && (current == null || current == 1L)
                && !StringUtils.hasText(keyword)
                && !StringUtils.hasText(city)
                && !StringUtils.hasText(starName)
                && !StringUtils.hasText(brand)
                && !StringUtils.hasText(priceRange);
    }

    public HotelSearchService.HotelSearchResult get() {
        HotelSearchService.HotelSearchResult localResult = localCache.getIfPresent(LOCAL_CACHE_KEY);
        if (localResult != null) {
            return localResult;
        }

        String json = stringRedisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(json)) {
            return null;
        }

        try {
            HotelSearchService.HotelSearchResult redisResult = objectMapper.readValue(json, HotelSearchService.HotelSearchResult.class);
            localCache.put(LOCAL_CACHE_KEY, redisResult);
            return redisResult;
        } catch (JsonProcessingException e) {
            stringRedisTemplate.delete(redisKey);
            return null;
        }
    }

    public void put(HotelSearchService.HotelSearchResult result) {
        if (result == null) {
            return;
        }
        localCache.put(LOCAL_CACHE_KEY, result);
        try {
            stringRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(result), redisTtl);
        } catch (JsonProcessingException ignored) {
        }
    }

    public void evict() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictNow();
                }
            });
            return;
        }
        evictNow();
    }

    private void evictNow() {
        localCache.invalidate(LOCAL_CACHE_KEY);
        stringRedisTemplate.delete(redisKey);
        purgeNginxCache();
    }

    private void purgeNginxCache() {
        if (!nginxPurgeEnabled) {
            return;
        }
        if (nginxCachePath == null || !Files.exists(nginxCachePath)) {
            log.warn("Nginx首页缓存目录不存在，跳过清理：{}", nginxCachePath);
            return;
        }
        try (var paths = Files.walk(nginxCachePath)) {
            long deleted = paths
                    .filter(Files::isRegularFile)
                    .mapToLong(this::deleteCacheFile)
                    .sum();
            log.info("已清理Nginx首页缓存文件数量：{}", deleted);
        } catch (IOException e) {
            log.warn("清理Nginx首页缓存失败，缓存目录：{}", nginxCachePath, e);
        }
    }

    private long deleteCacheFile(Path path) {
        try {
            Files.deleteIfExists(path);
            return 1;
        } catch (IOException e) {
            log.warn("删除Nginx缓存文件失败：{}", path, e);
            return 0;
        }
    }
}
