package com.travel.user.config;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MerchantApplicationRabbitConfig {

    public static final String EXCHANGE = "merchant.application.exchange";
    public static final String QUEUE = "merchant.application.queue";
    public static final String ROUTING_KEY = "merchant.application.submit";

    // 生产者声明交换机，后续发送商户申请消息都投递到这里。
    @Bean
    public DirectExchange merchantApplicationExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    // 生产者声明队列，保证首次发送消息前 RabbitMQ 中已有队列。
    @Bean
    public Queue merchantApplicationQueue() {
        return new Queue(QUEUE, true);
    }

    // 用明确的路由键把申请消息从交换机绑定到审核队列。
    @Bean
    public Binding merchantApplicationBinding(@Qualifier("merchantApplicationQueue") Queue merchantApplicationQueue,
                                              @Qualifier("merchantApplicationExchange") DirectExchange merchantApplicationExchange) {
        return BindingBuilder.bind(merchantApplicationQueue)
                .to(merchantApplicationExchange)
                .with(ROUTING_KEY);
    }

    // 统一使用 JSON 序列化消息，生产者只需要发送申请人的 userId。
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
