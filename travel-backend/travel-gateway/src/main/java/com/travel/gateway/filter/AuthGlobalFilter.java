package com.travel.gateway.filter;

import com.travel.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String TOKEN_BLACKLIST_PREFIX = "auth:token:blacklist:";

    private static final String[] WHITE_LIST = {
            "/user/login",
            "/user/register",
            "/product/published",
            "/product/hotel/search",
            "/uploads/",
            "/doc.html",
            "/swagger-ui",
            "/v3/api-docs"
    };

    private final ReactiveStringRedisTemplate redisTemplate;


    public AuthGlobalFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }

        if (isBlockedExternalPath(path)) {
            return forbidden(exchange, "Access Denied");
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "未提供认证令牌");
        }

        if (!JwtUtils.validateToken(token)) {
            return unauthorized(exchange, "认证令牌无效或已过期");
        }

        if (path.startsWith("/api/user/merchants")) {
            try {
                Claims claims = JwtUtils.parseToken(token);
                String roles = claims.get("roles", String.class);
                boolean allowed = StringUtils.hasText(roles)
                        && (roles.contains("ROLE_MERCHANT") || roles.contains("ROLE_ADMIN"));
                if (!allowed) {
                    return forbidden(exchange, "Access Denied");
                }
            } catch (JwtException | IllegalArgumentException e) {
                return unauthorized(exchange, "Token invalid or expired");
            }
        }

        return redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return unauthorized(exchange, "认证令牌已失效，请重新登录");
                    }
                    return chain.filter(exchange);
        });
    }

    private boolean isBlockedExternalPath(String path) {
        return "/api/user/balance/deduct".equals(path) || "/user/balance/deduct".equals(path);
    }

    private boolean isWhitePath(String path) {
        for (String whitePath : WHITE_LIST) {
            if (path.startsWith(whitePath) || path.startsWith("/api" + whitePath)) {
                return true;
            }
        }
        return false;
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        return response.writeWith(Mono.just(
                response.bufferFactory().wrap(body.getBytes())));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":403,\"message\":\"" + message + "\",\"data\":null}";
        return response.writeWith(Mono.just(
                response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
