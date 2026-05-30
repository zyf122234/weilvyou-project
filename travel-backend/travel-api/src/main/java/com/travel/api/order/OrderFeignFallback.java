package com.travel.api.order;

import com.travel.common.result.PageResult;
import com.travel.common.result.R;
import org.springframework.stereotype.Component;

@Component
public class OrderFeignFallback implements OrderFeignClient {

    @Override
    public R<OrderVO> getMyOrder(String authorization, Long id) {
        return R.fail(503, "订单服务暂时不可用，请稍后再试");
    }

    @Override
    public R<PageResult<OrderVO>> listMyOrders(String authorization, Long current, Long size) {
        return R.fail(503, "订单服务暂时不可用，请稍后再试");
    }

    @Override
    public R<PageResult<OrderVO>> listAllOrders(Long current, Long size, Integer status, String keyword) {
        return R.fail(503, "订单服务暂时不可用，请稍后再试");
    }
}
