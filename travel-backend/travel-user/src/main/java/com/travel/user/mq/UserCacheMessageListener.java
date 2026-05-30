package com.travel.user.mq;

import com.travel.user.config.UserCacheRabbitConfig;
import com.travel.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCacheMessageListener {

    private final UserService userService;

    @RabbitListener(queues = UserCacheRabbitConfig.LOGIN_QUEUE)
    public void warmupUserCache(UserCacheWarmupMessage message) {
        if (message == null || message.getUserId() == null) {
            return;
        }
        try {
            userService.getUserById(message.getUserId());
        } catch (Exception e) {
            log.warn("登录后预热用户缓存失败，用户编号={}", message.getUserId(), e);
        }
    }
}
