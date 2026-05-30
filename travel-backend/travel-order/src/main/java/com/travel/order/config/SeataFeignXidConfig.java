package com.travel.order.config;

import feign.RequestInterceptor;
import io.seata.core.context.RootContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class SeataFeignXidConfig {

    @Bean
    public RequestInterceptor seataXidHeaderInterceptor() {
        return template -> {
            String xid = RootContext.getXID();
            if (StringUtils.hasText(xid)) {
                template.header(RootContext.KEY_XID, xid);
            }
        };
    }
}
