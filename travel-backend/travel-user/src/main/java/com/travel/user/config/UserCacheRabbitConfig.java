package com.travel.user.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserCacheRabbitConfig {

    public static final String EXCHANGE = "user.cache.exchange";
    public static final String LOGIN_QUEUE = "user.cache.login.queue";
    public static final String LOGIN_ROUTING_KEY = "user.cache.login";

    @Bean
    public DirectExchange userCacheExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue userCacheLoginQueue() {
        return new Queue(LOGIN_QUEUE, true);
    }

    @Bean
    public Binding userCacheLoginBinding(@Qualifier("userCacheLoginQueue") Queue userCacheLoginQueue,
                                         @Qualifier("userCacheExchange") DirectExchange userCacheExchange) {
        return BindingBuilder.bind(userCacheLoginQueue)
                .to(userCacheExchange)
                .with(LOGIN_ROUTING_KEY);
    }
}
