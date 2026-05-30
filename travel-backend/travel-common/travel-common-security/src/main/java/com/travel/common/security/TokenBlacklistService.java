package com.travel.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    public static final String TOKEN_BLACKLIST_PREFIX = "auth:token:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    public void blacklist(String token, Duration ttl) {
        if (!StringUtils.hasText(token) || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(buildKey(token), "1", ttl);
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildKey(token)));
    }

    public static String buildKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + token;
    }
}
