package com.travel.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.api.order.OrderCreateDTO;
import com.travel.api.order.OrderVO;
import com.travel.api.product.ProductFeignClient;
import com.travel.api.product.ProductVO;
import com.travel.api.user.UserBalanceDeductDTO;
import com.travel.api.user.UserFeignClient;
import com.travel.api.user.UserVO;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.R;
import com.travel.common.utils.PageUtils;
import com.travel.order.entity.TravelOrder;
import com.travel.order.mapper.TravelOrderMapper;
import com.travel.order.service.OrderService;
import feign.FeignException;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int STATUS_PENDING_PAYMENT = 0;
    private static final int STATUS_PAID = 1;
    private static final int STATUS_CANCELED = 2;

    private final TravelOrderMapper orderMapper;
    private final ProductFeignClient productFeignClient;
    private final UserFeignClient userFeignClient;

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, OrderCreateDTO dto) {
        requireLogin(userId);

        ProductVO product = requirePublishedProduct(dto.getProductId());
        TravelOrder order = new TravelOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setProductCoverUrl(product.getCoverUrl());
        order.setMerchantId(product.getMerchantId());
        order.setMerchantName(product.getMerchantName());
        order.setDateStart(dto.getDateStart());
        order.setDateEnd(dto.getDateEnd());
        order.setQuantity(dto.getQuantity());
        order.setUnitPrice(scale(product.getPrice()));
        order.setTotalPrice(resolveTotalPrice(product.getPrice(), dto.getQuantity(), dto.getTotalPrice()));
        order.setContactName(dto.getContactName().trim());
        order.setPhone(dto.getPhone().trim());
        order.setStatus(STATUS_PENDING_PAYMENT);

        orderMapper.insert(order);
        return toVO(orderMapper.selectById(order.getId()));
    }

    @Override
    public OrderVO getMyOrder(Long userId, Long orderId) {
        return toVO(requireMyOrder(userId, orderId));
    }

    @Override
    @Transactional
    @GlobalTransactional(name = "order-pay", rollbackFor = Exception.class)
    public OrderVO payOrder(Long userId, Long orderId) {
        TravelOrder order = requireMyOrder(userId, orderId);
        if (order.getStatus() != null && order.getStatus() == STATUS_PAID) {
            return toVO(order);
        }
        if (order.getStatus() == null || order.getStatus() != STATUS_PENDING_PAYMENT) {
            throw new BusinessException(400, "当前订单状态不能支付");
        }

        deductUserBalance(order);
        order.setStatus(STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return toVO(orderMapper.selectById(order.getId()));
    }

    @Override
    @Transactional
    public OrderVO cancelOrder(Long userId, Long orderId) {
        TravelOrder order = requireMyOrder(userId, orderId);
        if (order.getStatus() != null && order.getStatus() == STATUS_CANCELED) {
            return toVO(order);
        }
        if (order.getStatus() != null && order.getStatus() == STATUS_PAID) {
            throw new BusinessException(400, "已支付订单不能取消");
        }
        if (order.getStatus() == null || order.getStatus() != STATUS_PENDING_PAYMENT) {
            throw new BusinessException(400, "当前订单状态不能取消");
        }

        order.setStatus(STATUS_CANCELED);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return toVO(orderMapper.selectById(order.getId()));
    }

    @Override
    public PageResult<OrderVO> listMyOrders(Long userId, Long current, Long size) {
        requireLogin(userId);
        IPage<TravelOrder> page = orderMapper.selectPage(
                new Page<>(PageUtils.safeCurrent(current), PageUtils.safeSize(size)),
                new LambdaQueryWrapper<TravelOrder>()
                        .eq(TravelOrder::getUserId, userId)
                        .orderByDesc(TravelOrder::getCreateTime));
        return PageResult.of(
                page.getTotal(),
                page.getPages(),
                page.getCurrent(),
                page.getSize(),
                page.getRecords().stream().map(this::toVO).toList());
    }

    //获取所有订单
    @Override
    public PageResult<OrderVO> listAllOrders(Long current, Long size, Integer status, String keyword) {
        LambdaQueryWrapper<TravelOrder> wrapper = new LambdaQueryWrapper<TravelOrder>()
                .eq(status != null, TravelOrder::getStatus, status)
                .and(StringUtils.hasText(keyword), item -> item
                        .like(TravelOrder::getOrderNo, keyword)
                        .or()
                        .like(TravelOrder::getProductName, keyword)
                        .or()
                        .like(TravelOrder::getMerchantName, keyword)
                        .or()
                        .like(TravelOrder::getContactName, keyword)
                        .or()
                        .like(TravelOrder::getPhone, keyword))
                .orderByDesc(TravelOrder::getCreateTime);
        IPage<TravelOrder> page = orderMapper.selectPage(new Page<>(PageUtils.safeCurrent(current), PageUtils.safeSize(size)), wrapper);
        Map<Long, UserVO> userMap = loadUserMap(page.getRecords());
        List<OrderVO> records = page.getRecords().stream()
                .map(order -> toVO(order, userMap.get(order.getUserId())))
                .toList();
        return PageResult.of(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public PageResult<OrderVO> listMerchantOrders(Long merchantId, Long current, Long size, Integer status, String keyword) {
        requireLogin(merchantId);
        LambdaQueryWrapper<TravelOrder> wrapper = new LambdaQueryWrapper<TravelOrder>()
                .eq(TravelOrder::getMerchantId, merchantId)
                .eq(status != null, TravelOrder::getStatus, status)
                .and(StringUtils.hasText(keyword), item -> item
                        .like(TravelOrder::getOrderNo, keyword)
                        .or()
                        .like(TravelOrder::getProductName, keyword)
                        .or()
                        .like(TravelOrder::getContactName, keyword)
                        .or()
                        .like(TravelOrder::getPhone, keyword))
                .orderByDesc(TravelOrder::getCreateTime);
        IPage<TravelOrder> page = orderMapper.selectPage(new Page<>(PageUtils.safeCurrent(current), PageUtils.safeSize(size)), wrapper);
        Map<Long, UserVO> userMap = loadUserMap(page.getRecords());
        List<OrderVO> records = page.getRecords().stream()
                .map(order -> toVO(order, userMap.get(order.getUserId())))
                .toList();
        return PageResult.of(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), records);
    }

    private TravelOrder requireMyOrder(Long userId, Long orderId) {
        requireLogin(userId);
        if (orderId == null) {
            throw new BusinessException(400, "订单ID不能为空");
        }
        TravelOrder order = orderMapper.selectOne(new LambdaQueryWrapper<TravelOrder>()
                .eq(TravelOrder::getId, orderId)
                .eq(TravelOrder::getUserId, userId));
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }

    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
    }

    //查询已发布的商品
    private ProductVO requirePublishedProduct(Long productId) {
        R<ProductVO> response;
        try {
            response = productFeignClient.getPublishedDetail(productId);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(404, "商品不存在或未发布");
        } catch (FeignException e) {
            throw new BusinessException("商品服务不可用");
        }
        if (response == null || response.getCode() == null || response.getCode() != 200 || response.getData() == null) {
            throw new BusinessException("商品服务不可用或商品不存在");
        }
        return response.getData();
    }

    private BigDecimal resolveTotalPrice(BigDecimal unitPrice, Integer quantity, BigDecimal clientTotalPrice) {
        BigDecimal calculated = scale(unitPrice).multiply(BigDecimal.valueOf(quantity));
        if (clientTotalPrice == null) {
            return scale(calculated);
        }
        BigDecimal submitted = scale(clientTotalPrice);
        if (submitted.compareTo(calculated) != 0) {
            throw new BusinessException(400, "订单金额与商品价格不一致");
        }
        return submitted;
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateOrderNo() {
        return "TO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private OrderVO toVO(TravelOrder order) {
        return toVO(order, null);
    }

    private OrderVO toVO(TravelOrder order, UserVO user) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setUserNickname(user.getNickname());
        }
        vo.setProductId(order.getProductId());
        vo.setProductName(order.getProductName());
        vo.setProductCoverUrl(order.getProductCoverUrl());
        vo.setMerchantId(order.getMerchantId());
        vo.setMerchantName(order.getMerchantName());
        vo.setDateStart(order.getDateStart());
        vo.setDateEnd(order.getDateEnd());
        vo.setQuantity(order.getQuantity());
        vo.setUnitPrice(order.getUnitPrice());
        vo.setTotalPrice(order.getTotalPrice());
        vo.setContactName(order.getContactName());
        vo.setPhone(order.getPhone());
        vo.setStatus(order.getStatus());
        vo.setStatusText(resolveStatusText(order.getStatus()));
        vo.setPayTime(order.getPayTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        return vo;
    }

    //批量查询用户
    private Map<Long, UserVO> loadUserMap(List<TravelOrder> orders) {
        List<Long> userIds = orders.stream()
                .map(TravelOrder::getUserId) //获取所有用户ID
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userIds.stream()
                .map(this::findUser)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UserVO::getId, Function.identity(), (left, right) -> left));
    }

    //通过id查询用户
    private UserVO findUser(Long userId) {
        try {
            R<UserVO> response = userFeignClient.getUserById(userId);
            if (response != null && response.getCode() != null && response.getCode() == 200) {
                return response.getData();
            }
        } catch (FeignException e) {
            return null;
        }
        return null;
    }

    private void deductUserBalance(TravelOrder order) {
        UserBalanceDeductDTO dto = new UserBalanceDeductDTO();
        dto.setAmount(scale(order.getTotalPrice()));
        try {
            R<UserVO> response = userFeignClient.deductBalance(order.getUserId(), dto);
            if (response == null) {
                throw new BusinessException("用户服务不可用");
            }
            if (response.getCode() == null || response.getCode() != 200) {
                throw new BusinessException(response.getCode() == null ? 500 : response.getCode(),
                        StringUtils.hasText(response.getMessage()) ? response.getMessage() : "余额扣减失败");
            }
        } catch (FeignException e) {
            throw new BusinessException("用户服务不可用");
        }
    }

    private String resolveStatusText(Integer status) {
        if (status != null && status == STATUS_PAID) {
            return "已支付";
        }
        if (status != null && status == STATUS_CANCELED) {
            return "已取消";
        }
        return "待支付";
    }

}
