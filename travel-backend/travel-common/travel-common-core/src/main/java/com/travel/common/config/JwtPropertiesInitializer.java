package com.travel.common.config;

import com.travel.common.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtPropertiesInitializer {

    public JwtPropertiesInitializer(@Value("${travel.jwt.secret}") String secret,
                                    @Value("${travel.jwt.expiration-ms}") long expirationMillis) {
        JwtUtils.configure(secret, expirationMillis);
    }
}
