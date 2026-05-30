package com.travel.order.task;

import com.travel.order.mapper.TravelOrderMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CanceledOrderCleanupTask {

    private static final int STATUS_CANCELED = 2;
    private static final long RETENTION_MINUTES = 5;

    private final TravelOrderMapper orderMapper;

    // 每5分钟执行一次
    @XxlJob("deleteExpiredCanceledOrders")
    public void deleteExpiredCanceledOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(RETENTION_MINUTES);
        int deleted = orderMapper.deleteCanceledBefore(STATUS_CANCELED, deadline);
        if (deleted > 0) {
            log.info("已删除{}个超过保留时间的已取消订单，截止时间：{}", deleted, deadline);
        }
    }
}
