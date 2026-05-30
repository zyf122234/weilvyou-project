package com.travel.common.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 专属的Feign 请求拦截器配置类
 * 用于在微服务间调用时，自动透传当前请求的 Authorization 头信息
 */
@Configuration
public class FeignHeaderConfig {

    /**
     * RequestInterceptor 就是 OpenFeign 专用的请求拦截器
     * 创建授权头请求拦截器 Bean
     * 该拦截器会在每次 Feign 发起远程调用前执行，将当前 HTTP 请求中的 Authorization 头复制到 Feign 请求中
     *
     * @return RequestInterceptor 实例
     */
    @Bean
    public RequestInterceptor authorizationHeaderInterceptor() {
        return template -> {
            // 获取当前请求的上下文属性
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            // 如果不在 Web 请求上下文中（例如定时任务或异步线程），则直接返回，避免空指针异常
            if (attributes == null) {
                return;
            }
            
            // 获取当前的 HttpServletRequest 对象
            HttpServletRequest request = attributes.getRequest();
            
            // 从当前请求中提取 Authorization 头信息
            String authorization = request.getHeader("Authorization");
            
            // 如果 Authorization 头存在且非空，则将其添加到 Feign 请求模板中
            if (authorization != null && !authorization.isBlank()) {
                template.header("Authorization", authorization);
            }
        };
    }
}
