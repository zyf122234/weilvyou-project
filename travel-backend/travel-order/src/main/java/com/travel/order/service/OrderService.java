package com.travel.order.service;

import com.travel.api.order.OrderCreateDTO;
import com.travel.api.order.OrderVO;
import com.travel.common.result.PageResult;

public interface OrderService {

    OrderVO createOrder(Long userId, OrderCreateDTO dto);

    OrderVO getMyOrder(Long userId, Long orderId);

    OrderVO payOrder(Long userId, Long orderId);

    OrderVO cancelOrder(Long userId, Long orderId);

    PageResult<OrderVO> listMyOrders(Long userId, Long current, Long size);

    PageResult<OrderVO> listAllOrders(Long current, Long size, Integer status, String keyword);

    PageResult<OrderVO> listMerchantOrders(Long merchantId, Long current, Long size, Integer status, String keyword);
}
