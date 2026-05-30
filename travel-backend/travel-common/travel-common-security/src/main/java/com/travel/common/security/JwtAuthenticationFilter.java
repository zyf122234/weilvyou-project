package com.travel.common.security;

import com.travel.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 微服务内 JWT 二次认证过滤器。
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 放行路径
    private final CommonSecurityProperties securityProperties;
    // redis黑名单
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(CommonSecurityProperties securityProperties,
                                   TokenBlacklistService tokenBlacklistService) {
        this.securityProperties = securityProperties;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    // 检查是否需要JWT验证
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String pattern : securityProperties.getWhiteList()) {
            if (matches(path, pattern)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // 检查token是否在redis黑名单
                if (tokenBlacklistService.isBlacklisted(token)) {
                    SecurityContextHolder.clearContext();
                    unauthorized(response, "认证令牌已失效，请重新登录");
                    return;
                }

                Claims claims = JwtUtils.parseToken(token);
                Long userId = claims.get("userId", Long.class);
                String username = claims.get("username", String.class);
                String roles = claims.get("roles", String.class);
                String loginSessionId = JwtUtils.resolveLoginSessionId(claims);

                // 获取角色列表
                List<SimpleGrantedAuthority> authorities = Arrays.stream(StringUtils.hasText(roles) ? roles.split(",") : new String[0])
                        .map(role -> new SimpleGrantedAuthority(role.trim()))
                        .collect(Collectors.toList());

                // 创建认证对象
                LoginUser loginUser = new LoginUser(userId, username, loginSessionId);
                // 设置认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                // 将认证对象放到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 匹配路径
    private boolean matches(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        return path.equals(pattern) || path.startsWith(pattern);
    }
}
