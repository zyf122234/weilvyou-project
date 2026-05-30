package com.travel.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 微服务认证公共配置。
 */
//它既有“默认值”，又支持“动态扩展”,如果配置了“travel.security.white-list”属性，则使用配置的属性值，否则使用默认值。
// 会将 yml文件中的 travel.security.white-list 追加/绑定到这个 whiteList 集合里
@ConfigurationProperties(prefix = "travel.security")
public class CommonSecurityProperties {

    private final List<String> whiteList = new ArrayList<>(List.of(
            "/user/login",
            "/user/register",
            "/uploads/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html"
    ));

    public List<String> getWhiteList() {
        return whiteList;
    }
}
