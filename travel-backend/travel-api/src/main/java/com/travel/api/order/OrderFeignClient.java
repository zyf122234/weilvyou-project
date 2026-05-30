package com.travel.api.order;

import com.travel.common.result.PageResult;
import com.travel.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 订单服务 Feign 接口
 */
@FeignClient(contextId = "orderFeignClient", name = "travel-order", path = "/order/orders", fallback = OrderFeignFallback.class)
public interface OrderFeignClient {

    @GetMapping("/{id}")
    R<OrderVO> getMyOrder(@RequestHeader(value = "Authorization", required = false) String authorization,
                          @PathVariable("id") Long id);

    @GetMapping("/mine")
    R<PageResult<OrderVO>> listMyOrders(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestParam(value = "current", defaultValue = "1") Long current,
                                        @RequestParam(value = "size", defaultValue = "5") Long size);

    /**
     * 查询所有订单（管理端）
     */
    @GetMapping("/admin/list")
    R<PageResult<OrderVO>> listAllOrders(@RequestParam(value = "current", defaultValue = "1") Long current,
                                          @RequestParam(value = "size", defaultValue = "10") Long size,
                                          @RequestParam(value = "status", required = false) Integer status,
                                          @RequestParam(value = "keyword", required = false) String keyword);
}
